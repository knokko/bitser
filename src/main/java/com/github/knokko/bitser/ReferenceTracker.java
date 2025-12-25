package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.exceptions.ReferenceBitserException;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

class ReferenceTracker {

	private final HashMap<String, LabelTargets> labels = new HashMap<>();
	final BitserCache cache;

	final ArrayList<WithStructJob> structJobs = new ArrayList<>();
	final ArrayList<WithArrayJob> arrayJobs = new ArrayList<>();

	ReferenceTracker(BitserCache cache) {
		this.cache = cache;
	}

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

	LabelTargets get(ReferenceFieldWrapper referenceWrapper) {
		if (referenceWrapper instanceof StableReferenceFieldWrapper) {
			BitStructWrapper<?> valueInfo = cache.getWrapperOrNull(referenceWrapper.field.type);
			if (valueInfo == null) {
				assert referenceWrapper.field.type != null;
				String className = referenceWrapper.field.type.getSimpleName();
				throw new InvalidBitFieldException("Can't extract stable ID from " + className + " because it's not a BitStruct");
			}
			if (!valueInfo.hasStableId()) {
				String className = valueInfo.constructor.getDeclaringClass().getSimpleName();
				throw new InvalidBitFieldException(className + " doesn't have an @StableReferenceFieldId");
			}
		}

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
			if (referenceWrapper.field.optional) {
				output.prepareProperty("optional", -1);
				output.write(reference != null);
				output.finishProperty();
				if (reference == null) return;
			} else if (reference == null) throw new InvalidBitValueException("Reference must not be null");

			if (referenceWrapper instanceof StableReferenceFieldWrapper) {
				BitStructWrapper<?> valueInfo = cache.getWrapperOrNull(referenceWrapper.field.type);
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
			if (ReadHelper.readOptional(input, referenceWrapper.field.optional)) return null;
			if (referenceWrapper instanceof StableReferenceFieldWrapper) return getStable(input);
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
