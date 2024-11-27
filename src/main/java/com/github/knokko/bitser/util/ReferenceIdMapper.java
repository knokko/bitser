package com.github.knokko.bitser.util;

import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.field.ReferenceFieldTarget;
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

	public ReferenceIdMapper(Set<String> labels) {
		this.labelMappings = new HashMap<>(labels.size());
		for (String label : labels) this.labelMappings.put(label, new Mappings());
	}

	public void register(ReferenceFieldTarget referenceTarget, Object value, BitserCache cache) {
		if (readOnly) throw new IllegalStateException("This mapper has become read-only");

		Mappings mappings = this.labelMappings.get(referenceTarget.label());
		if (mappings == null) throw new Error(
				"Unexpected target label " + referenceTarget.label() + ": expected one of " + labelMappings.keySet()
		);
		mappings.register(value, referenceTarget.stable(), cache);
	}

	public void encodeUnstableId(String label, Object value, BitOutputStream output) throws IOException {
		if (!readOnly) throw new IllegalStateException("You can't call encodeUnstableId until the mapper is read-only");

		Mappings mappings = this.labelMappings.get(label);
		if (mappings == null) throw new InvalidBitFieldException(
				"Can't find unstable @ReferenceFieldTarget with label " + label +
						": supported labels are " + labelMappings.keySet()
		);

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
				"Can't find stable @ReferenceFieldTarget with label " + label +
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

	public void save(BitOutputStream output) throws IOException {
		readOnly = true;

		String[] sortedLabels = new String[labelMappings.size()];
		int index = 0;
		for (String label : labelMappings.keySet()) {
			sortedLabels[index] = label;
			index += 1;
		}
		Arrays.sort(sortedLabels);

		for (String label: sortedLabels) {
			encodeVariableInteger(labelMappings.get(label).unstable.size(), 0, Integer.MAX_VALUE, output);
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

		final Map<IdWrapper, Integer> unstable = new HashMap<>();
		final Map<UUID, Object> stable = new HashMap<>();

		void register(Object target, boolean isStable, BitserCache cache) {
			if (isStable) {
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
			} else {
				IdWrapper wrapper = new IdWrapper(target);
				if (unstable.containsKey(wrapper)) throw new InvalidBitValueException(
						"Multiple unstable targets have identity " + target
				);
				unstable.put(wrapper, unstable.size());
			}
		}
	}
}
