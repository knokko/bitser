package com.github.knokko.bitser;

import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.util.RecursorException;

import java.util.ArrayList;

class Serializer {

	final BitserCache cache;
	final BitOutputStream output;

	final ArrayList<WriteStructJob> structJobs = new ArrayList<>();
	final ArrayList<WriteArrayJob> arrayJobs = new ArrayList<>();
	final ArrayList<WriteCollectionJob> collectionJobs = new ArrayList<>();
	final ArrayList<WriteStructReferenceJob> structReferenceJobs = new ArrayList<>();
	final ArrayList<WriteArrayReferenceJob> arrayReferenceJobs = new ArrayList<>();
	final ArrayList<WriteCollectionReferenceJob> collectionReferenceJobs = new ArrayList<>();

	final ReferenceTracker references;

	Serializer(BitserCache cache, BitOutputStream output, Object rootStruct) {
		this.cache = cache;
		this.output = output;
		BitStructWrapper<?> rootStructInfo = cache.getWrapper(rootStruct.getClass());
		this.structJobs.add(new WriteStructJob(
				rootStruct, rootStructInfo,
				new RecursionNode(rootStructInfo.constructor.getDeclaringClass().getSimpleName())
		));
		this.references = new ReferenceTracker(cache);
	}

	void run() {
		while (!structJobs.isEmpty() || !arrayJobs.isEmpty() || !collectionJobs.isEmpty()) {
			if (!structJobs.isEmpty()) {
				structJobs.remove(structJobs.size() - 1).write(this);
			}
			if (!arrayJobs.isEmpty()) {
				WriteArrayJob job = arrayJobs.remove(arrayJobs.size() - 1);
				try {
					job.write(this);
				} catch (Throwable failed) {
					throw new RecursorException(job.node.generateTrace(null), failed);
				}
			}
			if (!collectionJobs.isEmpty()) {
				WriteCollectionJob job = collectionJobs.remove(collectionJobs.size() - 1);
				try {
					job.write(this);
				} catch (Throwable failed) {
					throw new RecursorException(job.node.generateTrace(null), failed);
				}
			}
		}

		for (WriteStructReferenceJob referenceJob : structReferenceJobs) {
			try {
				referenceJob.save(this);
			} catch (Throwable failed) {
				throw new RecursorException(referenceJob.node.generateTrace(null), failed);
			}
		}
		structReferenceJobs.clear();

		for (WriteArrayReferenceJob referenceJob : arrayReferenceJobs) {
			try {
				referenceJob.save(this);
			} catch (Throwable failed) {
				throw new RecursorException(referenceJob.node.generateTrace(null), failed);
			}
		}
		arrayReferenceJobs.clear();

		for (WriteCollectionReferenceJob referenceJob : collectionReferenceJobs) {
			try {
				referenceJob.save(this);
			} catch (Throwable failed) {
				throw new RecursorException(referenceJob.node.generateTrace(null), failed);
			}
		}
		collectionReferenceJobs.clear();
	}
}
