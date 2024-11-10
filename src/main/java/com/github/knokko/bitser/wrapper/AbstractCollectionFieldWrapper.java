package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.BitserCache;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;

import static com.github.knokko.bitser.serialize.IntegerBitser.*;
import static com.github.knokko.bitser.serialize.IntegerBitser.decodeVariableInteger;
import static java.lang.Math.max;
import static java.lang.Math.min;

abstract class AbstractCollectionFieldWrapper extends BitFieldWrapper {

	private final IntegerField sizeField;

	AbstractCollectionFieldWrapper(BitField.Properties properties, IntegerField sizeField, Field classField) {
		super(properties, classField);
		if (sizeField.minValue() > Integer.MAX_VALUE) throw new IllegalArgumentException();
		if (sizeField.maxValue() < 0) throw new IllegalArgumentException();
		if (!properties.type.isArray() && (properties.type.isInterface() || Modifier.isAbstract(properties.type.getModifiers()))) {
			throw new InvalidBitFieldException("Field type must not be abstract or an interface: " + classField);
		}
		this.sizeField = sizeField;
	}

	@Override
	void writeValue(Object value, BitOutputStream output, BitserCache cache) throws IOException, IllegalAccessException {
		int size = getCollectionSize(value);
		if (sizeField.expectUniform()) encodeUniformInteger(size, getMinSize(), getMaxSize(), output);
		else encodeVariableInteger(size, getMinSize(), getMaxSize(), output);

		writeValue(value, size, output, cache);
	}

	@Override
	Object readValue(BitInputStream input, BitserCache cache) throws IOException, IllegalAccessException {
		int size;
		if (sizeField.expectUniform()) size = (int) decodeUniformInteger(getMinSize(), getMaxSize(), input);
		else size = (int) decodeVariableInteger(getMinSize(), getMaxSize(), input);

		Object value = constructCollectionWithSize(size);
		readValue(value, size, input, cache);
		return value;
	}

	abstract void writeValue(
			Object value, int size, BitOutputStream output, BitserCache cache
	) throws IOException, IllegalAccessException;

	abstract void readValue(
			Object value, int size, BitInputStream input, BitserCache cache
	) throws IOException, IllegalAccessException;

	private int getCollectionSize(Object object) {
		if (object instanceof Collection<?>) return ((Collection<?>) object).size();
		return Array.getLength(object);
	}

	private Object constructCollectionWithSize(int size) {
		if (properties.type.isArray()) {
			return Array.newInstance(properties.type.getComponentType(), size);
		} else {
			try {
				return properties.type.getConstructor(int.class).newInstance(size);
			} catch (NoSuchMethodException noIntConstructor) {
				try {
					return properties.type.getConstructor().newInstance();
				} catch (Exception unexpected) {
					throw new RuntimeException(unexpected);
				}
			} catch (Exception unexpected) {
				throw new RuntimeException(unexpected);
			}
		}
	}

	private int getMinSize() {
		return (int) max(0, sizeField.minValue());
	}

	private int getMaxSize() {
		return (int) min(Integer.MAX_VALUE, sizeField.maxValue());
	}
}
