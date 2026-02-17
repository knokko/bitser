package com.github.knokko.bitser;

import com.github.knokko.bitser.field.FunctionContext;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.options.CollectionSizeLimit;
import com.github.knokko.bitser.exceptions.RecursionException;

import java.util.ArrayList;
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
	final ArrayList<ReadStructMethodReferenceJob> structMethodReferenceJobs = new ArrayList<>();
	final ArrayList<ReadArrayReferenceJob> arrayReferenceJobs = new ArrayList<>();
	final ArrayList<ReadStructMethodReferenceToFieldJob> methodReferenceToFieldJobs = new ArrayList<>();
	final ArrayList<PopulateJob> populateJobs = new ArrayList<>();

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
		// Stage 1
		input.setMarker("stage 1: struct jobs & array jobs");
		while (!structJobs.isEmpty() || !arrayJobs.isEmpty()) {
			if (!structJobs.isEmpty()) {
				ReadStructJob structJob = structJobs.remove(structJobs.size() - 1);
				input.pushContext(structJob.node(), null);
				structJob.read(this);
				input.popContext(structJob.node(), null);
			}
			if (!arrayJobs.isEmpty()) {
				ReadArrayJob job = arrayJobs.remove(arrayJobs.size() - 1);
				try {
					input.pushContext(job.node(), null);
					job.read(this);
					input.popContext(job.node(), null);
				} catch (Throwable failed) {
					throw new RecursionException(job.node().generateTrace(null), failed);
				}
			}
		}

		// Stage 2
		input.setMarker("stage 2: with jobs");
		references.handleWithJobs(new FunctionContext(bitser, false, withParameters));

		// Stage 3
		input.setMarker("stage 3: map stable IDs");
		references.mapStableIDs();

		// Stage 4
		input.setMarker("stage 4: reference jobs");
		for (var referenceJob : structReferenceJobs) {
			try {
				input.pushContext(referenceJob.node(), null);
				referenceJob.resolve(this);
				input.popContext(referenceJob.node(), null);
			} catch (Throwable failed) {
				throw new RecursionException(referenceJob.node().generateTrace(null), failed);
			}
		}
		structReferenceJobs.clear();

		for (var referenceJob : structMethodReferenceJobs) {
			try {
				input.pushContext(referenceJob.node(), null);
				referenceJob.resolve(this);
				input.popContext(referenceJob.node(), null);
			} catch (Throwable failed) {
				throw new RecursionException(referenceJob.node().generateTrace(null), failed);
			}
		}
		structMethodReferenceJobs.clear();

		for (var referenceJob : arrayReferenceJobs) {
			try {
				input.pushContext(referenceJob.node(), null);
				referenceJob.resolve(this);
				input.popContext(referenceJob.node(), null);
			} catch (Throwable failed) {
				throw new RecursionException(referenceJob.node().generateTrace(null), failed);
			}
		}
		arrayReferenceJobs.clear();

		// Stages 5 and 6 are only used in backward-compatible deserialization

		// Stage 7
		input.setMarker("stage 7: method reference to field jobs");
		for (var referenceJob : methodReferenceToFieldJobs) {
			try {
				input.pushContext(referenceJob.node(), null);
				referenceJob.resolve();
				input.popContext(referenceJob.node(), null);
			} catch (Throwable failed) {
				throw new RecursionException(referenceJob.node().generateTrace(null), failed);
			}
		}
		methodReferenceToFieldJobs.clear();

		// Stages 8, 9, and 10
		input.setMarker("stage 8 to 10: collection population & post init");
		Populator.collectionsAndPostInit(populateJobs, postInitJobs);
	}
}
