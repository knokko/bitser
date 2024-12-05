package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.BitserCache;
import com.github.knokko.bitser.util.ReferenceIdLoader;
import com.github.knokko.bitser.util.ReferenceIdMapper;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Set;

abstract class BitFieldWrapper implements Comparable<BitFieldWrapper> {

	protected final BitField.Properties properties;
	protected final Field classField;

	BitFieldWrapper(BitField.Properties properties, Field classField) {
		this.properties = properties;
		this.classField = classField;
		if (Modifier.isStatic(classField.getModifiers()) && properties.ordering != -1) {
			throw new Error("Static fields should not have BitField annotation: " + classField);
		}
		if (!Modifier.isPublic(classField.getModifiers()) || Modifier.isFinal(classField.getModifiers())) {
			classField.setAccessible(true);
		}
		if (properties.optional && properties.type.isPrimitive()) {
			throw new InvalidBitFieldException("Primitive field " + classField + " can't be optional");
		}
	}

	@Override
	public int compareTo(BitFieldWrapper other) {
		return Integer.compare(this.properties.ordering, other.properties.ordering);
	}

	void collectReferenceTargetLabels(
			BitserCache cache, Set<String> declaredTargetLabels,
			Set<String> stableLabels, Set<String> unstableLabels, Set<Object> visitedObjects
	) {
		if (properties.referenceTarget != null) declaredTargetLabels.add(properties.referenceTarget.label());
	}

	void registerReferenceTargets(Object value, BitserCache cache, ReferenceIdMapper idMapper) {
		if (properties.referenceTarget != null && value != null) {
			idMapper.register(properties.referenceTarget, value, cache);
		}
	}

	void write(Object object, BitOutputStream output, BitserCache cache, ReferenceIdMapper idMapper) throws IOException {
		try {
			writeField(object, output, cache, idMapper);
		} catch (IllegalAccessException shouldNotHappen) {
			throw new Error(shouldNotHappen);
		} catch (InvalidBitValueException invalidValue) {
			throw new InvalidBitValueException(invalidValue.getMessage() + " for " + classField);
		}
	}

	void writeField(
			Object object, BitOutputStream output, BitserCache cache, ReferenceIdMapper idMapper
	) throws IOException, IllegalAccessException {
		Object value = classField.get(object);
		if (properties.optional) output.write(value != null);
		if (value == null) {
			if (!properties.optional) {
				throw new InvalidBitValueException("Field " + classField + " of " + object + " must not be null");
			}
		} else {
			writeValue(value, output, cache, idMapper);
			if (properties.referenceTarget != null) {
				idMapper.maybeEncodeUnstableId(properties.referenceTarget.label(), value, output);
			}
		}
	}

	abstract void writeValue(
			Object value, BitOutputStream output, BitserCache cache, ReferenceIdMapper idMapper
	) throws IOException, IllegalAccessException;

	void read(Object object, BitInputStream input, BitserCache cache, ReferenceIdLoader idLoader) throws IOException {
		try {
			readField(object, input, cache, idLoader);
		} catch (IllegalAccessException shouldNotHappen) {
			throw new Error(shouldNotHappen);
		}
	}

	void readField(
			Object object, BitInputStream input, BitserCache cache, ReferenceIdLoader idLoader
	) throws IOException, IllegalAccessException {
		if (properties.optional && !input.read()) classField.set(object, null);
		else {
			readValue(input, cache, idLoader, value -> classField.set(object, value));
			if (properties.referenceTarget != null) {
				idLoader.register(properties.referenceTarget.label(), classField.get(object), input, cache);
			}
		}
	}

	abstract void readValue(
			BitInputStream input, BitserCache cache, ReferenceIdLoader idLoader, ValueConsumer setValue
	) throws IOException, IllegalAccessException;
}
