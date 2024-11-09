package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.BitserCache;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

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
		if (properties.optional && classField.getType().isPrimitive()) {
			throw new InvalidBitFieldException("Primitive field " + classField + " can't be optional");
		}
	}

	@Override
	public int compareTo(BitFieldWrapper other) {
		return Integer.compare(this.properties.ordering, other.properties.ordering);
	}

	void write(Object object, BitOutputStream output, BitserCache cache) throws IOException {
		try {
			writeField(object, output, cache);
		} catch (IllegalAccessException shouldNotHappen) {
			throw new Error(shouldNotHappen);
		}
	}

	void writeField(Object object, BitOutputStream output, BitserCache cache) throws IOException, IllegalAccessException {
		Object value = classField.get(object);
		if (properties.optional) output.write(value != null);
		if (value == null) {
			if (!properties.optional) {
				throw new NullPointerException("Field " + classField + " of " + object + " must not be null");
			}
		} else writeValue(value, output, cache);
	}

	abstract void writeValue(Object value, BitOutputStream output, BitserCache cache) throws IOException, IllegalAccessException;

	void read(Object object, BitInputStream input, BitserCache cache) throws IOException {
		try {
			readField(object, input, cache);
		} catch (IllegalAccessException shouldNotHappen) {
			throw new Error(shouldNotHappen);
		}
	}

	void readField(Object object, BitInputStream input, BitserCache cache) throws IOException, IllegalAccessException {
		if (properties.optional && !input.read()) classField.set(object, null);
		else classField.set(object, readValue(input, cache));
	}

	abstract Object readValue(BitInputStream input, BitserCache cache) throws IOException, IllegalAccessException;
}
