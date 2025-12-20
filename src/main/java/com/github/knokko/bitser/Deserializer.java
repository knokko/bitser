package com.github.knokko.bitser;

import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.options.CollectionSizeLimit;
import com.github.knokko.bitser.util.RecursorException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;

class Deserializer {

	final Bitser bitser;
	final BitserCache cache;
	final BitInputStream input;
	final CollectionSizeLimit sizeLimit;
	final Map<String, Object> withParameters;
	final Object rootStruct;

	final ArrayList<ReadStructJob> structJobs = new ArrayList<>();
	final ArrayList<ReadArrayJob> arrayJobs = new ArrayList<>();
	final ArrayList<ReadStructReferenceJob> structReferenceJobs = new ArrayList<>();
	final ArrayList<ReadArrayReferenceJob> arrayReferenceJobs = new ArrayList<>();
	final ArrayList<PopulateCollectionJob> populateCollectionJobs = new ArrayList<>();

	final ArrayList<PostInitJob> postInitJobs = new ArrayList<>();

	final ReferenceTracker references;

	Deserializer(
			Bitser bitser, BitInputStream input,
			CollectionSizeLimit sizeLimit,
			Map<String, Object> withParameters,
			BitStructWrapper<?> rootStructInfo
	) {
		this.bitser = bitser;
		this.cache = bitser.cache;
		this.input = input;
		this.sizeLimit = sizeLimit;
		this.withParameters = withParameters;
		this.rootStruct = rootStructInfo.createEmptyInstance();
		this.structJobs.add(new ReadStructJob(
				rootStruct, rootStructInfo,
				new RecursionNode(rootStructInfo.constructor.getDeclaringClass().getSimpleName())
		));
		this.references = new ReferenceTracker(cache);
	}

	void run() {
		while (!structJobs.isEmpty() || !arrayJobs.isEmpty()) {
			if (!structJobs.isEmpty()) {
				ReadStructJob structJob = structJobs.remove(structJobs.size() - 1);
				input.pushContext(structJob.node, "(struct-job)");
				structJob.read(this);
				input.popContext(structJob.node, "(struct-job)");
			}
			if (!arrayJobs.isEmpty()) {
				ReadArrayJob job = arrayJobs.remove(arrayJobs.size() - 1);
				try {
					input.pushContext(job.node, "(array-job)");
					job.read(this);
					input.popContext(job.node, "(array-job)");
				} catch (Throwable failed) {
					throw new RecursorException(job.node.generateTrace(null), failed);
				}
			}
		}

		references.refreshStableIDs();
		references.handleWithJobs();

		for (ReadStructReferenceJob referenceJob : structReferenceJobs) {
			try {
				input.pushContext(referenceJob.node, "(struct-reference-job)");
				referenceJob.resolve(this);
				input.popContext(referenceJob.node, "(struct-reference-job)");
			} catch (Throwable failed) {
				throw new RecursorException(referenceJob.node.generateTrace(referenceJob.classField.getName()), failed);
			}
		}
		structReferenceJobs.clear();

		for (ReadArrayReferenceJob referenceJob : arrayReferenceJobs) {
			try {
				input.pushContext(referenceJob.node, "(array-reference-job)");
				referenceJob.resolve(this);
				input.popContext(referenceJob.node, "(array-reference-job)");
			} catch (Throwable failed) {
				throw new RecursorException(referenceJob.node.generateTrace(null), failed);
			}
		}
		arrayReferenceJobs.clear();

		// TODO Sort them by -depth
		populateCollectionJobs.sort(Comparator.comparingInt(a -> a.node.depth));
		for (PopulateCollectionJob populateJob : populateCollectionJobs) {
			try {
				populateJob.populate();
			} catch (Throwable failed) {
				throw new RecursorException(populateJob.node.generateTrace(null), failed);
			}
		}
		populateCollectionJobs.clear();

		for (PostInitJob postInitJob : postInitJobs) {
			postInitJob.structObject.postInit(postInitJob.context);
		}
		postInitJobs.clear();
	}
}
