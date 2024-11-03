package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.FloatField;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.BitserCache;

import java.io.IOException;
import java.lang.reflect.Field;

import static com.github.knokko.bitser.serialize.IntegerBitser.decodeUniformInteger;
import static com.github.knokko.bitser.serialize.IntegerBitser.encodeUniformInteger;

public class FloatFieldWrapper extends BitFieldWrapper {

	private final FloatField floatField;

	FloatFieldWrapper(BitField bitField, FloatField floatField, Field classField) {
		super(bitField, classField);
		this.floatField = floatField;
	}

	@Override
	void writeField(Object object, BitOutputStream output, BitserCache cache) throws IllegalAccessException, IOException {
		if (classField.getType() == float.class || classField.getType() == Float.class) {
			writeValue(classField.getFloat(object), output, cache);
		} else writeValue(classField.getDouble(object), output, cache);
	}

	@Override
	void writeValue(Object value, BitOutputStream output, BitserCache cache) throws IOException {
		// TODO Use expectMultipleOf
		if (value instanceof Float) {
			encodeUniformInteger(Float.floatToRawIntBits((Float) value), Integer.MIN_VALUE, Integer.MAX_VALUE, output);
		} else {
			encodeUniformInteger(Double.doubleToRawLongBits((Double) value), Long.MIN_VALUE, Long.MAX_VALUE, output);
		}
	}

	@Override
	void readField(Object object, BitInputStream input, BitserCache cache) throws IOException, IllegalAccessException {
		Object value = readValue(input, cache);
		if (value instanceof Float) classField.setFloat(object, (Float) value);
		else classField.setDouble(object, (Double) value);
	}

	@Override
	Object readValue(BitInputStream input, BitserCache cache) throws IOException {
		if (classField.getType() == float.class || classField.getType() == Float.class) {
			return Float.intBitsToFloat((int) decodeUniformInteger(Integer.MIN_VALUE, Integer.MAX_VALUE, input));
		} else {
			return Double.longBitsToDouble(decodeUniformInteger(Long.MIN_VALUE, Long.MAX_VALUE, input));
		}
	}
}
