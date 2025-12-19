package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.exceptions.ReferenceBitserException;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

class ReferenceTracker {

	final HashMap<String, LabelTargets> labels = new HashMap<>();
	final BitserCache cache;

	ReferenceTracker(BitserCache cache) {
		this.cache = cache;
	}

	void registerTarget(String label, Object target) {
		LabelTargets entries = labels.computeIfAbsent(label, key -> new LabelTargets(key, cache));
		entries.register(target);
	}

	LabelTargets get(ReferenceFieldWrapper referenceWrapper) {
		if (referenceWrapper instanceof StableReferenceFieldWrapper) {
			BitStructWrapper<?> valueInfo = cache.getWrapperOrNull(referenceWrapper.field.type);
			if (valueInfo == null) {
				throw new InvalidBitFieldException("Can't find stable ID of " + referenceWrapper.field.type + " because it's not a BitStruct");
			}
			if (!valueInfo.hasStableId()) {
				throw new InvalidBitFieldException("Can't find StableReferenceFieldId of " + valueInfo);
			}
		}

		LabelTargets targets = labels.get(referenceWrapper.label);
		if (targets == null) {
			if (referenceWrapper instanceof StableReferenceFieldWrapper) {
				throw new ReferenceBitserException("Can't find stable reference target with label " + referenceWrapper.label);
			} else {
				throw new ReferenceBitserException("Can't find unstable reference target with label " + referenceWrapper.label);
			}
		}
		return targets;
	}

	static class LabelTargets {

		final HashMap<IdentityWrapper, Integer> unstableToIDs = new HashMap<>();
		final ArrayList<Object> idsToUnstable = new ArrayList<>();
		final HashMap<UUID, Object> stable = new HashMap<>();

		final String myLabel;
		final BitserCache cache;

		LabelTargets(String myLabel, BitserCache cache) {
			this.myLabel = myLabel;
			this.cache = cache;
		}

		void register(Object target) {
			if (unstableToIDs.put(new IdentityWrapper(target), unstableToIDs.size()) != null) {
				throw new ReferenceBitserException("Multiple unstable targets have identity " + target);
			}
			idsToUnstable.add(target);

			BitStructWrapper<?> maybeWrapper = cache.getWrapperOrNull(target.getClass());
			if (maybeWrapper != null && maybeWrapper.hasStableId()) {
				UUID id = maybeWrapper.getStableId(target);
				if (stable.put(id, target) != null) throw new ReferenceBitserException("Duplicate stable target " + target);
			}
		}

		void save(ReferenceFieldWrapper referenceWrapper, Object reference, BitOutputStream output) throws Throwable {
			if (referenceWrapper.field.optional) {
				output.write(reference != null);
				if (reference == null) return;
			} else if (reference == null) throw new InvalidBitValueException("Reference must not be null");

			if (referenceWrapper instanceof StableReferenceFieldWrapper) {
				BitStructWrapper<?> valueInfo = cache.getWrapperOrNull(referenceWrapper.field.type);
				UUID id = valueInfo.getStableId(reference);
				IntegerBitser.encodeFullLong(id.getMostSignificantBits(), output);
				IntegerBitser.encodeFullLong(id.getLeastSignificantBits(), output);

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
				IntegerBitser.encodeUniformInteger(index, 0, unstableToIDs.size() - 1, output);
			}
		}

		Object get(ReferenceFieldWrapper referenceWrapper, BitInputStream input) throws Throwable {
			if (referenceWrapper.field.optional && !input.read()) return null;
			if (referenceWrapper instanceof StableReferenceFieldWrapper) return getStable(input);
			else return getUnstable(input);
		}

		Object getStable(BitInputStream input) throws Throwable {
			UUID id = new UUID(IntegerBitser.decodeFullLong(input), IntegerBitser.decodeFullLong(input));
			Object value = stable.get(id);
			if (value == null) {
				throw new ReferenceBitserException("Can't find stable reference target with label " + myLabel + " and ID " + id);
			}
			return value;
		}

		Object getUnstable(BitInputStream input) throws Throwable {
			int index = (int) IntegerBitser.decodeUniformInteger(0, unstableToIDs.size() - 1, input);
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
