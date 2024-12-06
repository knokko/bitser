package com.github.knokko.bitser.util;

import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.BitserCache;

import java.io.IOException;
import java.util.*;

import static com.github.knokko.bitser.serialize.IntegerBitser.*;

public class ReferenceIdMapper {

	static UUID extractStableId(Object target, BitserCache cache) {
		try {
			UUID id = cache.getWrapper(target.getClass()).getStableId(target);
			if (id == null) throw new InvalidBitValueException("Stable UUID of " + target + " is null, which is forbidden");
			return id;
		} catch (InvalidBitFieldException invalid) {
			if (invalid.getMessage().endsWith("is not a BitStruct")) {
				throw new InvalidBitFieldException("Can't extract stable id from " + target + " because it's not a BitStruct");
			} else {
				throw invalid;
			}
		}
	}

	private final Map<String, Mappings> labelMappings;

	private boolean readOnly;

	public ReferenceIdMapper(Set<String> declaredTargetLabels, Set<String> stableLabels, Set<String> unstableLabels) {
		this.labelMappings = new HashMap<>(declaredTargetLabels.size());
		for (String label : declaredTargetLabels) {
			this.labelMappings.put(label, new Mappings(stableLabels.contains(label), unstableLabels.contains(label)));
		}
	}

	public void register(String referenceTargetLabel, Object value, BitserCache cache) {
		if (readOnly) throw new IllegalStateException("This mapper has become read-only");

		Mappings mappings = this.labelMappings.get(referenceTargetLabel);
		if (mappings == null) throw new Error(
				"Unexpected target label " + referenceTargetLabel + ": expected one of " + labelMappings.keySet()
		);
		mappings.register(value, cache);
	}

	public void maybeEncodeUnstableId(String label, Object value, BitOutputStream output) throws IOException {
		if (!readOnly) throw new IllegalStateException("You can't call encodeUnstableId until the mapper is read-only");

		Mappings mappings = this.labelMappings.get(label);
		if (mappings == null) throw new InvalidBitFieldException(
				"Can't find @ReferenceFieldTarget with label " + label +
						": supported labels are " + labelMappings.keySet()
		);
		if (mappings.unstable == null) return;

		Integer id = mappings.unstable.get(new IdWrapper(value));
		if (id == null) throw new InvalidBitValueException(
				"Can't find unstable reference target with label " + label + " and value " + value
		);

		encodeUniformInteger(id, 0, mappings.unstable.size() - 1, output);
	}

	public void encodeStableId(String label, Object value, BitOutputStream output, BitserCache cache) throws IOException {
		if (!readOnly) throw new IllegalStateException("You can't call encodeStableId until the mapper is read-only");

		Mappings mappings = this.labelMappings.get(label);
		if (mappings == null) throw new InvalidBitFieldException(
				"Can't find @ReferenceFieldTarget with label " + label +
						": supported labels are " + labelMappings.keySet()
		);

		UUID id = extractStableId(value, cache);
		Object mappedValue = mappings.stable.get(id);
		if (mappedValue == null) throw new InvalidBitValueException(
				"Can't find stable reference target " + value + " with label " + label
		);
		if (mappedValue != value) throw new InvalidBitValueException(
				"Stable reference target " + value + " has the same ID as " + mappedValue
		);

		encodeUniformInteger(id.getMostSignificantBits(), Long.MIN_VALUE, Long.MAX_VALUE, output);
		encodeUniformInteger(id.getLeastSignificantBits(), Long.MIN_VALUE, Long.MAX_VALUE, output);
	}

	public void shareWith(ReferenceIdLoader loader) {
		readOnly = true;
		loader.prepareWith();

		labelMappings.forEach((label, mappings) -> {
			if (mappings.stable != null) mappings.stable.forEach((id, target) -> loader.withStable(label, id, target));
			if (mappings.unstable != null) mappings.unstable.forEach((wrapper, id) -> loader.withUnstable(label, id, wrapper.value));
		});
	}

	public void save(BitOutputStream output) throws IOException {
		readOnly = true;

		String[] sortedLabels = new String[labelMappings.size()];
		int index = 0;
		for (String label : labelMappings.keySet()) {
			sortedLabels[index] = label;
			index += 1;
		}
		Arrays.sort(sortedLabels);

		for (String label : sortedLabels) {
			Mappings mappings = labelMappings.get(label);
			if (mappings.unstable != null) {
				encodeVariableInteger(mappings.unstable.size(), 0, Integer.MAX_VALUE, output);
			}
		}
	}

	private static class IdWrapper {

		final Object value;

		IdWrapper(Object value) {
			this.value = value;
		}

		@Override
		public boolean equals(Object other) {
			if (other instanceof IdWrapper) {
				return this.value == ((IdWrapper) other).value;
			} else return false;
		}

		@Override
		public int hashCode() {
			return System.identityHashCode(value);
		}
	}

	private static class Mappings {

		final Map<UUID, Object> stable;
		final Map<IdWrapper, Integer> unstable;

		Mappings(boolean hasStable, boolean hasUnstable) {
			this.stable = hasStable ? new HashMap<>() : null;
			this.unstable = hasUnstable ? new HashMap<>() : null;
		}

		void register(Object target, BitserCache cache) {
			if (stable != null) {
				UUID targetId = extractStableId(target, cache);
				Object existing = stable.putIfAbsent(targetId, target);
				if (existing != null) {
					if (existing == target) throw new InvalidBitValueException(
							"Multiple stable targets have identity " + target
					);
					else throw new InvalidBitValueException(
							"Multiple objects (" + existing + " and " + target + ") have ID " + targetId
					);
				}
			}
			if (unstable != null) {
				IdWrapper wrapper = new IdWrapper(target);
				if (unstable.containsKey(wrapper)) throw new InvalidBitValueException(
						"Multiple unstable targets have identity " + target
				);
				unstable.put(wrapper, unstable.size());
			}
		}
	}
}
