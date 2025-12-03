package com.github.knokko.bitser.util;

import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.exceptions.ReferenceBitserException;
import com.github.knokko.bitser.exceptions.UnexpectedBitserException;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.serialize.BitserCache;
import com.github.knokko.bitser.serialize.CollectionSizeLimit;
import com.github.knokko.bitser.serialize.LabelCollection;
import com.github.knokko.bitser.wrapper.ValueConsumer;

import java.io.IOException;
import java.util.*;

import static com.github.knokko.bitser.serialize.IntegerBitser.decodeUniformInteger;
import static com.github.knokko.bitser.serialize.IntegerBitser.decodeVariableInteger;
import static com.github.knokko.bitser.util.ReferenceIdMapper.extractStableId;

public class ReferenceIdLoader {

	public static ReferenceIdLoader load(
			BitInputStream input, LabelCollection labels, CollectionSizeLimit sizeLimit
	) throws IOException {
		String[] sortedLabels = new String[labels.declaredTargets.size()];
		int index = 0;
		for (String label : labels.declaredTargets) {
			sortedLabels[index] = label;
			index += 1;
		}
		Arrays.sort(sortedLabels);

		Map<String, Mappings> labelMappings = new HashMap<>(labels.declaredTargets.size());
		for (String label : sortedLabels) {
			int unstableSize = 0;
			if (labels.unstable.contains(label)) {
				unstableSize = (int) decodeVariableInteger(0, Integer.MAX_VALUE, input);
				if (sizeLimit != null && unstableSize > sizeLimit.maxSize) {
					throw new InvalidBitValueException("Number of unstable targets (" + unstableSize + ") with label " +
							label + " exceeds the CollectionSizeLimit " + sizeLimit.maxSize);
				}
			}
			labelMappings.put(label, new Mappings(unstableSize, labels.stable.contains(label)));
		}

		return new ReferenceIdLoader(labelMappings);
	}

	private final Map<String, Mappings> labelMappings;
	private final List<Runnable> postResolveCallbacks = new ArrayList<>();

	private ReferenceIdLoader(Map<String, Mappings> labelMappings) {
		this.labelMappings = labelMappings;
	}

	public void register(String label, Object value, BitInputStream input, BitserCache cache) throws IOException {
		Mappings mappings = labelMappings.get(label);
		if (mappings == null) throw new ReferenceBitserException("Invalid bitstream: label " + label + " was never saved");

		if (mappings.stable != null) mappings.registerStable(value, extractStableId(value, cache));
		if (mappings.unstable != null) {
			mappings.registerUnstable(value, (int) decodeUniformInteger(0, mappings.unstableSize - 1, input));
		}
	}

	public void replace(String label, Object oldTarget, Object newTarget) {
		Mappings mappings = labelMappings.get(label);
		if (mappings != null) mappings.replace(oldTarget, newTarget);
	}

	void prepareWith() {
		labelMappings.values().forEach(Mappings::prepareWith);
	}

	void withStable(String label, UUID id, Object target) {
		Mappings mappings = labelMappings.get(label);
		if (mappings == null) throw new ReferenceBitserException("Invalid with: label " + label + " was never saved");

		mappings.registerStable(target, id);
	}

	void withUnstable(String label, int id, Object target) {
		Mappings mappings = labelMappings.get(label);
		if (mappings == null) throw new ReferenceBitserException("Invalid with: label " + label + " was never saved");

		mappings.registerUnstable(target, mappings.ownUnstableSize + id);
	}

	public void getUnstable(String label, ValueConsumer setValue, BitInputStream input) throws IOException {
		Mappings mappings = labelMappings.get(label);
		if (mappings == null) throw new ReferenceBitserException("Invalid bitstream: label " + label + " was never saved");

		mappings.getUnstable(label, (int) decodeUniformInteger(0, mappings.unstableSize - 1, input), setValue);
	}

	public void getStable(String label, ValueConsumer setValue, BitInputStream input) throws IOException {
		Mappings mappings = labelMappings.get(label);
		if (mappings == null) throw new ReferenceBitserException("Invalid bitstream: label " + label + " was never saved");

		UUID id = new UUID(
				decodeUniformInteger(Long.MIN_VALUE, Long.MAX_VALUE, input),
				decodeUniformInteger(Long.MIN_VALUE, Long.MAX_VALUE, input)
		);
		mappings.getStable(label, id, setValue);
	}

	public void addPostResolveCallback(Runnable callback) {
		postResolveCallbacks.add(callback);
	}

	public void resolve() throws IOException {
		for (Mappings mappings : labelMappings.values()) {
			for (ResolveTask task : mappings.resolveTasks) task.resolve();
		}
	}

	public void postResolve() {
		for (Runnable callback : postResolveCallbacks) callback.run();
	}

	private static class Mappings {

		final int unstableSize;
		final Map<Integer, Object> unstable;
		final Map<UUID, Object> stable;
		final Map<ReferenceIdMapper.IdWrapper, Object> replacements = new HashMap<>();
		final Collection<ResolveTask> resolveTasks = new ArrayList<>();

		int ownUnstableSize;

		Mappings(int unstableSize, boolean hasStable) {
			this.unstableSize = unstableSize;
			this.unstable = unstableSize > 0 ? new HashMap<>(unstableSize) : null;
			this.stable = hasStable ? new HashMap<>() : null;
		}

		void replace(Object oldTarget, Object newTarget) {
			replacements.put(new ReferenceIdMapper.IdWrapper(oldTarget), newTarget);
		}

		void registerUnstable(Object target, int id) {
			if (unstable.containsKey(id)) throw new UnexpectedBitserException("Duplicate id " + id);
			unstable.put(id, target);
		}

		void registerStable(Object target, UUID id) {
			if (stable.containsKey(id)) throw new ReferenceBitserException(
					"Multiple objects have id " + id + ": " + target + " and " + stable.get(id)
			);
			stable.put(id, target);
		}

		void getUnstable(String label, int unstableId, ValueConsumer setValue) {
			resolveTasks.add(() -> {
				Object value = unstable.get(unstableId);
				if (value == null) throw new ReferenceBitserException(
						"Reference with label " + label + " and id " + unstableId + " was never saved"
				);
				Object replacement = replacements.get(new ReferenceIdMapper.IdWrapper(value));
				if (replacement == null) setValue.consume(value);
				else setValue.consume(replacement);
			});
		}

		void getStable(String label, UUID id, ValueConsumer setValue) {
			resolveTasks.add(() -> {
				Object value = stable.get(id);
				if (value == null) throw new ReferenceBitserException(
						"Reference with label " + label + " and id " + id + " was never saved"
				);
				Object replacement = replacements.get(new ReferenceIdMapper.IdWrapper(value));
				if (replacement == null) setValue.consume(value);
				else setValue.consume(replacement);
			});
		}

		void prepareWith() {
			if (unstable != null) ownUnstableSize = unstable.size();
		}
	}

	@FunctionalInterface
	private interface ResolveTask {

		void resolve() throws IOException;
	}
}
