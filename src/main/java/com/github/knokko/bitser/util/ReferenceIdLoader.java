package com.github.knokko.bitser.util;

import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.serialize.BitserCache;
import com.github.knokko.bitser.wrapper.ValueConsumer;

import java.io.IOException;
import java.util.*;

import static com.github.knokko.bitser.serialize.IntegerBitser.decodeUniformInteger;
import static com.github.knokko.bitser.serialize.IntegerBitser.decodeVariableInteger;
import static com.github.knokko.bitser.util.ReferenceIdMapper.extractStableId;

public class ReferenceIdLoader {

	public static ReferenceIdLoader load(BitInputStream input, Set<String> labels) throws IOException {
		String[] sortedLabels = new String[labels.size()];
		int index = 0;
		for (String label : labels) {
			sortedLabels[index] = label;
			index += 1;
		}
		Arrays.sort(sortedLabels);

		Map<String, Mappings> labelMappings = new HashMap<>(labels.size());
		for (String label : sortedLabels) {
			labelMappings.put(label, new Mappings((int) decodeVariableInteger(0, Integer.MAX_VALUE, input)));
		}

		return new ReferenceIdLoader(labelMappings);
	}

	private final Map<String, Mappings> labelMappings;

	private ReferenceIdLoader(Map<String, Mappings> labelMappings) {
		this.labelMappings = labelMappings;
	}

	public void registerStable(String label, Object value, BitserCache cache) {
		Mappings mappings = labelMappings.get(label);
		if (mappings == null) throw new Error("Invalid bitstream: label " + label + " was never saved");

		mappings.registerStable(value, extractStableId(value, cache));
	}

	public void registerUnstable(String label, Object value, BitInputStream input) throws IOException {
		Mappings mappings = labelMappings.get(label);
		if (mappings == null) throw new Error("Invalid bitstream: label " + label + " was never saved");

		mappings.registerUnstable(value, (int) decodeUniformInteger(0, mappings.unstableSize - 1, input));
	}

	public void getUnstable(String label, ValueConsumer setValue, BitInputStream input) throws IOException {
		Mappings mappings = labelMappings.get(label);
		if (mappings == null) throw new Error("Invalid bitstream: label " + label + " was never saved");

		mappings.getUnstable(label, (int) decodeUniformInteger(0, mappings.unstableSize - 1, input), setValue);
	}

	public void getStable(String label, ValueConsumer setValue, BitInputStream input) throws IOException {
		Mappings mappings = labelMappings.get(label);
		if (mappings == null) throw new Error("Invalid bitstream: label " + label + " was never saved");

		UUID id = new UUID(
				decodeUniformInteger(Long.MIN_VALUE, Long.MAX_VALUE, input),
				decodeUniformInteger(Long.MIN_VALUE, Long.MAX_VALUE, input)
		);
		mappings.getStable(label, id, setValue);
	}

	public void resolve() throws IOException, IllegalAccessException {
		for (Mappings mappings : labelMappings.values()) {
			for (ResolveTask task : mappings.resolveTasks) task.resolve();
		}
	}

	private static class Mappings {

		final int unstableSize;
		final Map<Integer, Object> unstable;
		final Map<UUID, Object> stable = new HashMap<>();
		final Collection<ResolveTask> resolveTasks = new ArrayList<>();

		Mappings(int unstableSize) {
			this.unstableSize = unstableSize;
			this.unstable = new HashMap<>(unstableSize);
		}

		void registerUnstable(Object target, int id) {
			if (unstable.containsKey(id)) throw new Error("Duplicate id " + id);
			unstable.put(id, target);
		}

		void registerStable(Object target, UUID id) {
			if (stable.containsKey(id)) throw new Error("Duplicate id " + id);
			stable.put(id, target);
		}

		void getUnstable(String label, int unstableId, ValueConsumer setValue) {
			resolveTasks.add(() -> {
				Object value = unstable.get(unstableId);
				if (value == null) throw new Error(
						"Reference with label " + label + " and id " + unstableId + " was never saved"
				);
				setValue.consume(value);
			});
		}

		void getStable(String label, UUID id, ValueConsumer setValue) {
			resolveTasks.add(() -> {
				Object value = stable.get(id);
				if (value == null) throw new Error(
						"Reference with label " + label + " and id " + id + " was never saved"
				);
				setValue.consume(value);
			});
		}
	}

	@FunctionalInterface
	private interface ResolveTask {

		void resolve() throws IOException, IllegalAccessException;
	}
}
