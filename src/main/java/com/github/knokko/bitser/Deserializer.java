package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.ReferenceBitserException;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.options.CollectionSizeLimit;
import com.github.knokko.bitser.util.RecursorException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.UUID;

class Deserializer {

	final BitserCache cache;
	final BitInputStream input;
	final CollectionSizeLimit sizeLimit;
	final Object rootStruct;

	final ArrayList<ReadStructJob> structJobs = new ArrayList<>();
	final ArrayList<ReadCollectionJob> collectionJobs = new ArrayList<>();
	final HashMap<String, ArrayList<Object>> unstableReferenceTargets = new HashMap<>();
	final HashMap<String, HashMap<UUID, Object>> stableReferenceTargets = new HashMap<>();
	final ArrayList<ReadStructReferenceJob> structReferenceJobs = new ArrayList<>();
	final ArrayList<ReadCollectionReferenceJob> collectionReferenceJobs = new ArrayList<>();
	final ArrayList<PopulateCollectionJob> populateCollectionJobs = new ArrayList<>();

	Deserializer(
			BitserCache cache, BitInputStream input,
			CollectionSizeLimit sizeLimit,
			BitStructWrapper<?> rootStructInfo
	) {
		this.cache = cache;
		this.input = input;
		this.sizeLimit = sizeLimit;
		this.rootStruct = rootStructInfo.createEmptyInstance();
		this.structJobs.add(new ReadStructJob(
				rootStruct, rootStructInfo,
				new RecursionNode(rootStructInfo.constructor.getDeclaringClass().getSimpleName())
		));
	}

	void run() {
		while (!structJobs.isEmpty() || !collectionJobs.isEmpty()) {
			if (!structJobs.isEmpty()) {
				structJobs.remove(structJobs.size() - 1).read(this);
			}
			if (!collectionJobs.isEmpty()) {
				ReadCollectionJob job = collectionJobs.remove(collectionJobs.size() - 1);
				try {
					job.read(this);
				} catch (Throwable failed) {
					throw new RecursorException(job.node.generateTrace(null), failed);
				}
			}
		}

		for (ReadStructReferenceJob referenceJob : structReferenceJobs) {
			try {
				referenceJob.resolve(this);
			} catch (Throwable failed) {
				throw new RecursorException(referenceJob.node.generateTrace(referenceJob.classField.getName()), failed);
			}
		}
		structReferenceJobs.clear();

		for (ReadCollectionReferenceJob referenceJob : collectionReferenceJobs) {
			try {
				referenceJob.resolve(this);
			} catch (Throwable failed) {
				throw new RecursorException(referenceJob.node.generateTrace(null), failed);
			}
		}
		collectionReferenceJobs.clear();

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
	}

	void registerReferenceTarget(String label, Object target) {
		unstableReferenceTargets.computeIfAbsent(label, key -> new ArrayList<>()).add(target);
		BitStructWrapper<?> maybeWrapper = cache.getWrapperOrNull(target.getClass());
		if (maybeWrapper != null && maybeWrapper.hasStableId()) {
			UUID id = maybeWrapper.getStableId(target);
			HashMap<UUID, Object> stableMap = stableReferenceTargets.computeIfAbsent(label, key -> new HashMap<>());
			if (stableMap.put(id, target) != null) throw new ReferenceBitserException("Duplicate stable target");
		}
	}
}
