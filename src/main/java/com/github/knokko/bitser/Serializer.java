package com.github.knokko.bitser;

import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.distributions.FloatDistributionTracker;
import com.github.knokko.bitser.distributions.IntegerDistributionTracker;
import com.github.knokko.bitser.exceptions.RecursionException;

import java.util.ArrayList;
import java.util.Map;

class Serializer {

	final Bitser bitser;
	final BitserCache cache;
	final Map<String, Object> withParameters;
	final BitOutputStream output;
	final boolean backwardCompatible;

	final ArrayList<WriteStructJob> structJobs = new ArrayList<>();
	final ArrayList<WriteArrayJob> arrayJobs = new ArrayList<>();
	final ArrayList<WriteStructReferenceJob> structReferenceJobs = new ArrayList<>();
	final ArrayList<WriteArrayReferenceJob> arrayReferenceJobs = new ArrayList<>();

	final ReferenceTracker references;

	final boolean forbidLazySaving;
	final IntegerDistributionTracker intDistribution;
	final FloatDistributionTracker floatDistribution;

	Serializer(
			Bitser bitser, Map<String, Object> withParameters, BitOutputStream output, boolean backwardCompatible,
			Object rootStruct, boolean forbidLazySaving,
			IntegerDistributionTracker intDistribution, FloatDistributionTracker floatDistribution
	) {
		this.bitser = bitser;
		this.cache = bitser.cache;
		this.withParameters = withParameters;
		this.output = output;
		this.backwardCompatible = backwardCompatible;
		BitStructWrapper<?> rootStructInfo = cache.getWrapper(rootStruct.getClass());
		this.structJobs.add(new WriteStructJob(
				rootStruct, rootStructInfo,
				new RecursionNode(rootStructInfo.constructor.getDeclaringClass().getSimpleName())
		));
		this.references = new ReferenceTracker(cache);

		this.forbidLazySaving = forbidLazySaving;
		this.intDistribution = intDistribution;
		this.floatDistribution = floatDistribution;
	}

	void run() {
		while (!structJobs.isEmpty() || !arrayJobs.isEmpty()) {
			if (!structJobs.isEmpty()) {
				WriteStructJob job = structJobs.remove(structJobs.size() - 1);
				output.pushContext(job.node(), "(struct-job)");
				job.write(this);
				output.popContext(job.node(), "(struct-job)");
			}
			if (!arrayJobs.isEmpty()) {
				WriteArrayJob job = arrayJobs.remove(arrayJobs.size() - 1);
				try {
					output.pushContext(job.node(), "(array-job)");
					job.write(this);
					output.popContext(job.node(), "(array-job)");
				} catch (Throwable failed) {
					throw new RecursionException(job.node().generateTrace(null), failed);
				}
			}
		}

		references.handleWithJobs();

		for (WriteStructReferenceJob referenceJob : structReferenceJobs) {
			try {
				output.pushContext(referenceJob.node, "(struct-reference-job)");
				referenceJob.save(this);
				output.popContext(referenceJob.node, "(struct-reference-job)");
			} catch (Throwable failed) {
				throw new RecursionException(referenceJob.node.generateTrace(null), failed);
			}
		}
		structReferenceJobs.clear();

		for (WriteArrayReferenceJob referenceJob : arrayReferenceJobs) {
			try {
				output.pushContext(referenceJob.node, "(array-reference-job)");
				referenceJob.save(this);
				output.popContext(referenceJob.node, "(array-reference-job)");
			} catch (Throwable failed) {
				throw new RecursionException(referenceJob.node.generateTrace(null), failed);
			}
		}
		arrayReferenceJobs.clear();
	}
}
