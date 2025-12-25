package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.LegacyBitserException;
import com.github.knokko.bitser.exceptions.ReferenceBitserException;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.legacy.BackStructInstance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

class BackReferenceTracker {

	private final HashMap<String, LabelTargets> labels = new HashMap<>();
	private final HashMap<ReferenceTracker.IdentityWrapper, Object> legacyToModern = new HashMap<>();
	final BitserCache cache;

	BackReferenceTracker(BitserCache cache) {
		this.cache = cache;
	}

	// TODO Delete?
	void registerLegacyTarget(String label, Object target) {
		LabelTargets entries = labels.computeIfAbsent(label, LabelTargets::new);
		entries.registerLegacy(target);
	}

	void registerModern(Object legacyTarget, Object modernTarget) {
		if (legacyToModern.put(new ReferenceTracker.IdentityWrapper(legacyTarget), modernTarget) != null) {
			throw new LegacyBitserException("Multiple legacy reference targets have identity " + legacyTarget);
		}
	}

	void processStableLegacyIDs() {
		for (LabelTargets targets : labels.values()) {
			for (Object legacyTarget : targets.idsToLegacy) {
				if (legacyTarget instanceof BackStructInstance) {
					UUID stableID = ((BackStructInstance) legacyTarget).stableID;
					if (stableID != null) {
						if (targets.stable.put(stableID, (BackStructInstance) legacyTarget) != null) {
							throw new ReferenceBitserException("Multiple legacy stable targets have ID " + stableID);
						}
					}
				}
			}
		}
	}

	Object getModern(Object legacyReference) {
		Object modernReference = legacyToModern.get(new ReferenceTracker.IdentityWrapper(legacyReference));
		if (modernReference == null) throw new LegacyBitserException(
				"Reference target for " + legacyReference + " no longer exists"
		);
		return modernReference;
	}

	LabelTargets get(ReferenceFieldWrapper referenceWrapper) {
		LabelTargets targets = labels.get(referenceWrapper.label);
		if (targets == null) {
			throw new ReferenceBitserException(
					"Can't find legacy @ReferenceFieldTarget with label " + referenceWrapper.label
			);
		}
		return targets;
	}

	static class LabelTargets {

		private final String myLabel;

		private final HashMap<UUID, BackStructInstance> stable = new HashMap<>();
		final HashMap<ReferenceTracker.IdentityWrapper, Integer> legacyToIDs = new HashMap<>();
		final ArrayList<Object> idsToLegacy = new ArrayList<>();

		LabelTargets(String label) {
			this.myLabel = label;
		}

		void registerLegacy(Object target) {
			if (legacyToIDs.put(new ReferenceTracker.IdentityWrapper(target), legacyToIDs.size()) != null) {
				throw new ReferenceBitserException("Multiple legacy unstable targets have identity " + target);
			}
			idsToLegacy.add(target);
		}

		Object getLegacy(ReferenceFieldWrapper referenceWrapper, BitInputStream input) throws Throwable {
			// TODO Check whether the optional is saved twice, also check for normal ReferenceTracker
			if (referenceWrapper.field.optional && !input.read()) return null;
			if (referenceWrapper instanceof StableReferenceFieldWrapper) return getStable(input);
			else return getUnstable(input);
		}

		private BackStructInstance getStable(BitInputStream input) throws Throwable {
			input.prepareProperty("legacy-stable-id", -1);
			UUID id = new UUID(IntegerBitser.decodeFullLong(input), IntegerBitser.decodeFullLong(input));
			input.finishProperty();
			BackStructInstance value = stable.get(id);
			if (value == null) {
				throw new ReferenceBitserException(
						"Can't find legacy stable reference target with label " + myLabel + " and ID " + id
				);
			}
			return value;
		}

		private Object getUnstable(BitInputStream input) throws Throwable {
			input.prepareProperty("legacy-unstable-id", -1);
			int index = (int) IntegerBitser.decodeUniformInteger(0, legacyToIDs.size() - 1, input);
			input.finishProperty();
			return idsToLegacy.get(index);
		}
	}
}
