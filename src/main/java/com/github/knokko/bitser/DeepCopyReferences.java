package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.ReferenceBitserException;

import java.util.HashMap;
import java.util.Map;

class DeepCopyReferences {

	final Map<ReferenceTracker.IdentityWrapper, Object> ownedReferences = new HashMap<>();

	void register(Object original, Object target) {
		var key = new ReferenceTracker.IdentityWrapper(original);
		if (ownedReferences.put(key, target) != null) {
			throw new ReferenceBitserException("Multiple reference targets have identity " + original);
		}
	}

	Object convert(Object reference) {
		var key = new ReferenceTracker.IdentityWrapper(reference);
		var ownReference = ownedReferences.get(key);
		if (ownReference != null) return ownReference;
		else return reference;
	}
}
