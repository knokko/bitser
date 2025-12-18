package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.ReferenceBitserException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

class ReferenceTracker {

	final HashMap<String, ArrayList<Object>> unstableTargets = new HashMap<>();
	final HashMap<String, HashMap<UUID, Object>> stableTargets = new HashMap<>();

	final BitserCache cache;

	ReferenceTracker(BitserCache cache) {
		this.cache = cache;
	}

	void registerTarget(String label, Object target) {
		unstableTargets.computeIfAbsent(label, key -> new ArrayList<>()).add(target);
		BitStructWrapper<?> maybeWrapper = cache.getWrapperOrNull(target.getClass());
		if (maybeWrapper != null && maybeWrapper.hasStableId()) {
			UUID id = maybeWrapper.getStableId(target);
			HashMap<UUID, Object> stableMap = stableTargets.computeIfAbsent(label, key -> new HashMap<>());
			if (stableMap.put(id, target) != null) throw new ReferenceBitserException("Duplicate stable target");
		}
	}
}
