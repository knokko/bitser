package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.ReferenceBitserException;
import com.github.knokko.bitser.io.BitInputStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

class Deserializer {

	final BitserCache cache;
	final BitInputStream input;
	final Object rootStruct;

	final ArrayList<ReadStructJob> structJobs = new ArrayList<>();
	final ArrayList<ReadCollectionJob> collectionJobs = new ArrayList<>();
	final HashMap<String, ArrayList<Object>> unstableReferenceTargets = new HashMap<>();
	final HashMap<String, HashMap<UUID, Object>> stableReferenceTargets = new HashMap<>();
	final ArrayList<ReadStructReferenceJob> structReferenceJobs = new ArrayList<>();
	final ArrayList<ReadCollectionReferenceJob> collectionReferenceJobs = new ArrayList<>();
	final ArrayList<PopulateCollectionJob> populateCollectionJobs = new ArrayList<>();

	Deserializer(BitserCache cache, BitInputStream input, BitStructWrapper<?> rootStructInfo) {
		this.cache = cache;
		this.input = input;
		this.rootStruct = rootStructInfo.createEmptyInstance();
		this.structJobs.add(new ReadStructJob(rootStruct, rootStructInfo));
	}

	void run() {
		while (!structJobs.isEmpty() || !collectionJobs.isEmpty()) {
			if (!structJobs.isEmpty()) {
				structJobs.remove(structJobs.size() - 1).read(this);
			}
			if (!collectionJobs.isEmpty()) {
				collectionJobs.remove(collectionJobs.size() - 1).read(this);
			}
		}

		for (ReadStructReferenceJob referenceJob : structReferenceJobs) {
			referenceJob.resolve(this);
		}
		structReferenceJobs.clear();

		for (ReadCollectionReferenceJob referenceJob : collectionReferenceJobs) {
			referenceJob.resolve(this);
		}
		collectionReferenceJobs.clear();

		// TODO Sort them by -depth
		for (PopulateCollectionJob populateJob : populateCollectionJobs) {
			populateJob.populate();
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
