package com.github.knokko.bitser;

import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.legacy.BackStructInstance;
import com.github.knokko.bitser.options.CollectionSizeLimit;
import com.github.knokko.bitser.util.RecursorException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;

class BackDeserializer {

	final Bitser bitser;
	final BitserCache cache;
	final BitInputStream input;
	final LegacyClasses legacy;
	final CollectionSizeLimit sizeLimit;
	final Map<String, Object> withParameters;
	final Object rootStruct;

	final ArrayList<BackReadStructJob> structJobs = new ArrayList<>();
	final ArrayList<BackReadArrayJob> arrayJobs = new ArrayList<>();
	final ArrayList<BackReadStructReferenceJob> structReferenceJobs = new ArrayList<>();
	final ArrayList<BackReadArrayReferenceJob> arrayReferenceJobs = new ArrayList<>();
	final ArrayList<BackConvertStructJob> convertStructJobs = new ArrayList<>();
	final ArrayList<BackConvertArrayJob> convertArrayJobs = new ArrayList<>();
	final ArrayList<BackConvertStructReferenceJob> convertStructReferenceJobs = new ArrayList<>();
	final ArrayList<BackConvertArrayReferenceJob> convertArrayReferenceJobs = new ArrayList<>();
	final ArrayList<PopulateJob> populateJobs = new ArrayList<>();

	final BackReferenceTracker references;

	BackDeserializer(
			Bitser bitser, BitInputStream input, LegacyClasses legacy,
			CollectionSizeLimit sizeLimit,
			Map<String, Object> withParameters,
			BitStructWrapper<?> rootStructInfo
	) {
		this.bitser = bitser;
		this.cache = bitser.cache;
		this.input = input;
		this.legacy = legacy;
		this.sizeLimit = sizeLimit;
		this.withParameters = withParameters;
		this.rootStruct = rootStructInfo.createEmptyInstance();

		BackStructInstance legacyRootStruct = legacy.getRoot().constructEmptyInstance(0);
		this.structJobs.add(new BackReadStructJob(
				legacyRootStruct, legacy.getRoot(),
				new RecursionNode(rootStructInfo.constructor.getDeclaringClass().getSimpleName())
		));
		this.convertStructJobs.add(new BackConvertStructJob(
				rootStruct, rootStructInfo, legacyRootStruct,
				new RecursionNode(rootStructInfo.constructor.getDeclaringClass().getSimpleName())
		));

		this.references = new BackReferenceTracker(cache);
	}

	void run() {
		while (!structJobs.isEmpty() || !arrayJobs.isEmpty()) {
			if (!structJobs.isEmpty()) {
				BackReadStructJob structJob = structJobs.remove(structJobs.size() - 1);
				input.pushContext(structJob.node, "(back-struct-job)");
				structJob.read(this);
				input.popContext(structJob.node, "(back-struct-job)");
			}
			if (!arrayJobs.isEmpty()) {
				BackReadArrayJob job = arrayJobs.remove(arrayJobs.size() - 1);
				try {
					input.pushContext(job.node, "(array-job)");
					job.read(this);
					input.popContext(job.node, "(array-job)");
				} catch (Throwable failed) {
					throw new RecursorException(job.node.generateTrace(null), failed);
				}
			}
		}

		references.processStableLegacyIDs();

		for (BackReadStructReferenceJob referenceJob : structReferenceJobs) {
			try {
				input.pushContext(referenceJob.node, "(struct-reference-job)");
				referenceJob.resolve(this);
				input.popContext(referenceJob.node, "(struct-reference-job)");
			} catch (Throwable failed) {
				String topContext = "field/function " + referenceJob.legacyValuesIndex;
				throw new RecursorException(referenceJob.node.generateTrace(topContext), failed);
			}
		}
		structReferenceJobs.clear();

		for (BackReadArrayReferenceJob referenceJob : arrayReferenceJobs) {
			try {
				input.pushContext(referenceJob.node, "(array-reference-job)");
				referenceJob.resolve(this);
				input.popContext(referenceJob.node, "(array-reference-job)");
			} catch (Throwable failed) {
				throw new RecursorException(referenceJob.node.generateTrace(null), failed);
			}
		}
		arrayReferenceJobs.clear();

		while (!convertStructJobs.isEmpty() || !convertArrayJobs.isEmpty()) {
			if (!convertStructJobs.isEmpty()) {
				BackConvertStructJob job = convertStructJobs.remove(convertStructJobs.size() - 1);
				input.pushContext(job.node, "(back-convert-struct-job)");
				job.convert(this);
				input.popContext(job.node, "(back-convert-struct-job)");
			}
			if (!convertArrayJobs.isEmpty()) {
				BackConvertArrayJob job = convertArrayJobs.remove(convertArrayJobs.size() - 1);
				try {
					input.pushContext(job.node, "(back-convert-array-job)");
					job.convert(this);
					input.popContext(job.node, "(back-convert-array-job)");
				} catch (Throwable failed) {
					throw new RecursorException(job.node.generateTrace(null), failed);
				}
			}
		}

		for (BackConvertStructReferenceJob job : convertStructReferenceJobs) {
			try {
				job.convert(this);
			} catch (Throwable failed) {
				throw new RecursorException(job.node.generateTrace(null), failed);
			}
		}
		convertStructReferenceJobs.clear();

		for (BackConvertArrayReferenceJob job : convertArrayReferenceJobs) {
			try {
				job.convert(this);
			} catch (Throwable failed) {
				throw new RecursorException(job.node.generateTrace(null), failed);
			}
		}
		convertArrayReferenceJobs.clear();

		// TODO Sort them by -depth, and create nasty unit test with reference (target) keys
		populateJobs.sort(Comparator.comparingInt(a -> a.node.depth));
		for (PopulateJob populateJob : populateJobs) {
			try {
				populateJob.populate();
			} catch (Throwable failed) {
				throw new RecursorException(populateJob.node.generateTrace(null), failed);
			}
		}
		populateJobs.clear();
	}
}
