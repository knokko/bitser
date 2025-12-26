package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.LegacyBitserException;
import com.github.knokko.bitser.exceptions.ReferenceBitserException;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.legacy.BackReference;
import com.github.knokko.bitser.legacy.BackStructInstance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

class BackReferenceTracker extends AbstractReferenceTracker {

	private final HashMap<String, LabelTargets> labels = new HashMap<>();
	private final HashMap<ReferenceTracker.IdentityWrapper, Object> legacyToModern = new HashMap<>();

	BackReferenceTracker(BitserCache cache) {
		super(cache);
	}

	@Override
	void registerTarget(String label, Object target) {
		labels.computeIfAbsent(label, LabelTargets::new).registerWith(target, cache);
	}

	void setWithObjects(List<Object> withObjects) {
		for (Object withObject : withObjects) {
			BitStructWrapper<?> structInfo = cache.getWrapper(withObject.getClass());
			RecursionNode node = new RecursionNode("with " + withObject.getClass().getSimpleName());
			structJobs.add(new WithStructJob(withObject, structInfo, node));
		}
	}

	void registerLegacyTarget(String label, Object target) {
		labels.computeIfAbsent(label, LabelTargets::new).registerLegacy(target);
	}

	void handleWithJobs() {
		while (!structJobs.isEmpty() || !arrayJobs.isEmpty()) {
			if (!structJobs.isEmpty()) structJobs.remove(structJobs.size() - 1).register(this);
			if (!arrayJobs.isEmpty()) arrayJobs.remove(arrayJobs.size() - 1).register(this);
		}
	}

	void registerModern(Object legacyTarget, Object modernTarget) {
		if (legacyToModern.put(new ReferenceTracker.IdentityWrapper(legacyTarget), modernTarget) != null) {
			throw new LegacyBitserException("Multiple legacy reference targets have identity " + legacyTarget);
		}
	}

	void processStableLegacyIDs() {
		for (LabelTargets targets : labels.values()) {
			for (Object rawLegacyTarget : targets.idsToLegacyOrWith) {
				Object legacyTarget = ((BackReference) rawLegacyTarget).reference;
				if (legacyTarget instanceof BackStructInstance) {
					UUID stableID = ((BackStructInstance) legacyTarget).stableID;
					if (stableID != null) {
						if (targets.stable.put(stableID, new BackReference(legacyTarget)) != null) {
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

	Object getWithOrLegacy(ReferenceFieldWrapper referenceWrapper, BitInputStream input) throws Throwable {
		LabelTargets targets = labels.get(referenceWrapper.label);
		if (targets == null) {
			throw new ReferenceBitserException(
					"Can't find legacy @ReferenceFieldTarget with label " + referenceWrapper.label
			);
		}
		return targets.getWithOrLegacy(referenceWrapper, input);
	}

	static class LabelTargets {

		private final String myLabel;

		private final HashMap<UUID, Object> stable = new HashMap<>();
		final HashMap<ReferenceTracker.IdentityWrapper, Integer> legacyOrWithToIDs = new HashMap<>();
		final ArrayList<Object> idsToLegacyOrWith = new ArrayList<>();

		LabelTargets(String label) {
			this.myLabel = label;
		}

		void registerLegacy(Object target) {
			if (legacyOrWithToIDs.put(new ReferenceTracker.IdentityWrapper(target), legacyOrWithToIDs.size()) != null) {
				throw new ReferenceBitserException("Multiple legacy unstable targets have identity " + target);
			}
			idsToLegacyOrWith.add(new BackReference(target));
		}

		void registerWith(Object target, BitserCache cache) {
			if (legacyOrWithToIDs.put(new ReferenceTracker.IdentityWrapper(target), legacyOrWithToIDs.size()) != null) {
				throw new ReferenceBitserException("Multiple legacy unstable targets have identity " + target);
			}
			idsToLegacyOrWith.add(new WithReference(target));
			BitStructWrapper<?> maybeWrapper = cache.getWrapperOrNull(target.getClass());
			if (maybeWrapper != null && maybeWrapper.hasStableId()) {
				UUID id = maybeWrapper.getStableId(target);
				Object withSameID = stable.put(id, new WithReference(target));
				if (withSameID != null) {
					if (((WithReference) withSameID).reference == target) {
						throw new ReferenceBitserException("Multiple stable targets have identity " + target);
					} else {
						throw new ReferenceBitserException(
								"Multiple stable targets have ID " + id + ": " + target + " and " + withSameID
						);
					}
				}
			}
		}

		Object getWithOrLegacy(ReferenceFieldWrapper referenceWrapper, BitInputStream input) throws Throwable {
			if (referenceWrapper.stable) return getStable(input);
			else return getUnstable(input);
		}

		private Object getStable(BitInputStream input) throws Throwable {
			input.prepareProperty("legacy-stable-id", -1);
			UUID id = new UUID(IntegerBitser.decodeFullLong(input), IntegerBitser.decodeFullLong(input));
			input.finishProperty();
			Object value = stable.get(id);
			if (value == null) {
				throw new ReferenceBitserException(
						"Can't find legacy stable reference target with label " + myLabel + " and ID " + id
				);
			}
			return value;
		}

		private Object getUnstable(BitInputStream input) throws Throwable {
			input.prepareProperty("legacy-unstable-id", -1);
			int index = (int) IntegerBitser.decodeUniformInteger(0, legacyOrWithToIDs.size() - 1, input);
			input.finishProperty();
			return idsToLegacyOrWith.get(index);
		}
	}
}
