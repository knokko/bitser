package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.BitserCache;
import com.github.knokko.bitser.util.ReferenceIdLoader;
import com.github.knokko.bitser.util.ReferenceIdMapper;
import com.github.knokko.bitser.util.VirtualField;

import java.io.IOException;
import java.util.Set;

abstract class BitFieldWrapper implements Comparable<BitFieldWrapper> {

	protected final VirtualField field;

	BitFieldWrapper(VirtualField field) {
		this.field = field;
		if (field.optional && field.type.isPrimitive()) {
			throw new InvalidBitFieldException("Primitive field " + field + " can't be optional");
		}
	}

	@Override
	public int compareTo(BitFieldWrapper other) {
		return Integer.compare(this.field.ordering, other.field.ordering);
	}

	void collectReferenceTargetLabels(
			BitserCache cache, Set<String> declaredTargetLabels,
			Set<String> stableLabels, Set<String> unstableLabels, Set<Object> visitedObjects
	) {
		if (field.referenceTargetLabel != null) declaredTargetLabels.add(field.referenceTargetLabel);
	}

	void registerReferenceTargets(Object value, BitserCache cache, ReferenceIdMapper idMapper) {
		if (field.referenceTargetLabel != null && value != null) {
			idMapper.register(field.referenceTargetLabel, value, cache);
		}
	}

	void write(Object object, BitOutputStream output, BitserCache cache, ReferenceIdMapper idMapper) throws IOException {
		try {
			writeField(object, output, cache, idMapper);
		} catch (InvalidBitValueException invalidValue) {
			throw new InvalidBitValueException(invalidValue.getMessage() + " for " + field);
		}
	}

	void writeField(
			Object object, BitOutputStream output, BitserCache cache, ReferenceIdMapper idMapper
	) throws IOException {
		Object value = field.getValue.apply(object);
		if (field.optional) output.write(value != null);
		if (value == null) {
			if (!field.optional) {
				throw new InvalidBitValueException("Field " + field + " of " + object + " must not be null");
			}
		} else {
			writeValue(value, output, cache, idMapper);
			if (field.referenceTargetLabel != null) {
				idMapper.maybeEncodeUnstableId(field.referenceTargetLabel, value, output);
			}
		}
	}

	abstract void writeValue(
			Object value, BitOutputStream output, BitserCache cache, ReferenceIdMapper idMapper
	) throws IOException;

	void readField(
			Object object, BitInputStream input, BitserCache cache, ReferenceIdLoader idLoader
	) throws IOException {
		if (field.optional && !input.read()) field.setValue.accept(object, null);
		else {
			readValue(input, cache, idLoader, value -> field.setValue.accept(object, value));
			if (field.referenceTargetLabel != null) {
				idLoader.register(field.referenceTargetLabel, field.getValue.apply(object), input, cache);
			}
		}
	}

	abstract void readValue(
			BitInputStream input, BitserCache cache, ReferenceIdLoader idLoader, ValueConsumer setValue
	) throws IOException;
}
