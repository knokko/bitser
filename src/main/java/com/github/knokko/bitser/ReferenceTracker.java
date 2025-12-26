package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.ReferenceBitserException;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

class ReferenceTracker extends AbstractReferenceTracker {

	private final HashMap<String, LabelTargets> labels = new HashMap<>();

	ReferenceTracker(BitserCache cache) {
		super(cache);
	}

	@Override
	void registerTarget(String label, Object target) {
		LabelTargets entries = labels.computeIfAbsent(label, key -> new LabelTargets(key, cache));
		entries.register(target);
	}

	void setWithObjects(List<Object> withObjects) {
		for (Object withObject : withObjects) {
			BitStructWrapper<?> structInfo = cache.getWrapper(withObject.getClass());
			RecursionNode node = new RecursionNode("with " + withObject.getClass().getSimpleName());
			structJobs.add(new WithStructJob(withObject, structInfo, node));
		}
	}

	void handleWithJobs() {
		while (!structJobs.isEmpty() || !arrayJobs.isEmpty()) {
			if (!structJobs.isEmpty()) structJobs.remove(structJobs.size() - 1).register(this);
			if (!arrayJobs.isEmpty()) arrayJobs.remove(arrayJobs.size() - 1).register(this);
		}
	}

	void refreshStableIDs() {
		for (LabelTargets targets : labels.values()) {
			ArrayList<Object> stableTargets = new ArrayList<>(targets.stable.values());
			targets.stable.clear();
			for (Object target : stableTargets) targets.registerStable(target);
		}
	}

	Object get(ReferenceFieldWrapper referenceWrapper, BitInputStream input) throws Throwable {
		return get(referenceWrapper).get(referenceWrapper, input);
	}

	void save(ReferenceFieldWrapper referenceWrapper, Object reference, BitOutputStream output) throws Throwable {
		get(referenceWrapper).save(referenceWrapper, reference, output);
	}

	private LabelTargets get(ReferenceFieldWrapper referenceWrapper) {
		if (referenceWrapper.stable) cache.requireStableID(referenceWrapper.field.type);

		LabelTargets targets = labels.get(referenceWrapper.label);
		if (targets == null) {
			throw new ReferenceBitserException("Can't find @ReferenceFieldTarget with label " + referenceWrapper.label);
		}
		return targets;
	}

	static class LabelTargets {

		final HashMap<IdentityWrapper, Integer> unstableToIDs = new HashMap<>();
		final ArrayList<Object> idsToUnstable = new ArrayList<>();
		final HashMap<UUID, Object> stable = new HashMap<>();

		final String myLabel;
		final BitserCache cache;

		private LabelTargets(String myLabel, BitserCache cache) {
			this.myLabel = myLabel;
			this.cache = cache;
		}

		private void registerUnstable(Object target) {
			if (unstableToIDs.put(new IdentityWrapper(target), unstableToIDs.size()) != null) {
				throw new ReferenceBitserException("Multiple unstable targets have identity " + target);
			}
			idsToUnstable.add(target);
		}

		private void registerStable(Object target) {
			BitStructWrapper<?> maybeWrapper = cache.getWrapperOrNull(target.getClass());
			if (maybeWrapper != null && maybeWrapper.hasStableId()) {
				UUID id = maybeWrapper.getStableId(target);
				Object withSameID = stable.put(id, target);
				if (withSameID != null) {
					if (withSameID == target) {
						throw new ReferenceBitserException("Multiple stable targets have identity " + target);
					} else {
						throw new ReferenceBitserException(
								"Multiple stable targets have ID " + id + ": " + target + " and " + withSameID
						);
					}
				}
			}
		}

		void register(Object target) {
			registerStable(target);
			registerUnstable(target);
		}

		void save(ReferenceFieldWrapper referenceWrapper, Object reference, BitOutputStream output) throws Throwable {
			if (referenceWrapper.stable) {
				BitStructWrapper<?> valueInfo = cache.getWrapperOrNull(reference.getClass());
				UUID id = valueInfo.getStableId(reference);
				output.prepareProperty("stable-id", -1);
				IntegerBitser.encodeFullLong(id.getMostSignificantBits(), output);
				IntegerBitser.encodeFullLong(id.getLeastSignificantBits(), output);
				output.finishProperty();

				Object expectedTarget = stable.get(id);
				if (expectedTarget == null) {
					throw new ReferenceBitserException("Can't find stable reference target with ID " + id + " and label " + myLabel);
				}
				if (expectedTarget != reference) {
					throw new ReferenceBitserException("Expected reference with ID " + id + " to be " + expectedTarget + ", but was " + reference);
				}
			} else {
				Integer index = unstableToIDs.get(new IdentityWrapper(reference));
				if (index == null) {
					throw new ReferenceBitserException("Can't find unstable reference target " + reference + " with label " + myLabel);
				}
				output.prepareProperty("unstable-id", -1);
				IntegerBitser.encodeUniformInteger(index, 0, unstableToIDs.size() - 1, output);
				output.finishProperty();
			}
		}

		Object get(ReferenceFieldWrapper referenceWrapper, BitInputStream input) throws Throwable {
			if (referenceWrapper.stable) return getStable(input);
			else return getUnstable(input);
		}

		private Object getStable(BitInputStream input) throws Throwable {
			input.prepareProperty("stable-id", -1);
			UUID id = new UUID(IntegerBitser.decodeFullLong(input), IntegerBitser.decodeFullLong(input));
			input.finishProperty();
			Object value = stable.get(id);
			if (value == null) {
				throw new ReferenceBitserException("Can't find stable reference target with label " + myLabel + " and ID " + id);
			}
			return value;
		}

		private Object getUnstable(BitInputStream input) throws Throwable {
			input.prepareProperty("unstable-id", -1);
			int index = (int) IntegerBitser.decodeUniformInteger(0, unstableToIDs.size() - 1, input);
			input.finishProperty();
			return idsToUnstable.get(index);
		}
	}

	static class IdentityWrapper {

		final Object target;

		IdentityWrapper(Object target) {
			this.target = target;
		}

		@Override
		public int hashCode() {
			return System.identityHashCode(target);
		}

		@Override
		public boolean equals(Object other) {
			return other instanceof IdentityWrapper && this.target == ((IdentityWrapper) other).target;
		}
	}
}
