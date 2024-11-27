package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.BitserCache;
import com.github.knokko.bitser.util.ReferenceIdLoader;
import com.github.knokko.bitser.util.ReferenceIdMapper;

import java.io.IOException;
import java.lang.reflect.Field;

import static com.github.knokko.bitser.serialize.IntegerBitser.*;
import static java.lang.Long.max;
import static java.lang.Long.min;

public class IntegerFieldWrapper extends BitFieldWrapper {

	private final IntegerField intField;

	IntegerFieldWrapper(BitField.Properties properties, IntegerField intField, Field classField) {
		super(properties, classField);
		this.intField = intField;
	}

	@Override
	void writeValue(Object fatValue, BitOutputStream output, BitserCache cache, ReferenceIdMapper idMapper) throws IOException {
		long value = ((Number) fatValue).longValue();
		if (intField.expectUniform()) encodeUniformInteger(value, getMinValue(), getMaxValue(), output);
		else encodeVariableInteger(value, getMinValue(), getMaxValue(), output);
	}

	@Override
	void readValue(
			BitInputStream input, BitserCache cache, ReferenceIdLoader idLoader, ValueConsumer setValue
	) throws IOException, IllegalAccessException {
		long longValue;
		if (intField.expectUniform()) longValue = decodeUniformInteger(getMinValue(), getMaxValue(), input);
		else longValue = decodeVariableInteger(getMinValue(), getMaxValue(), input);

		Class<?> type = properties.type;
		if (type == byte.class || type == Byte.class) setValue.consume((byte) longValue);
		else if (type == short.class || type == Short.class) setValue.consume((short) longValue);
		else if (type == int.class || type == Integer.class) setValue.consume((int) longValue);
		else setValue.consume(longValue);
	}

	private long getMinValue() {
		long minValue = intField.minValue();

		long classMinValue = Long.MIN_VALUE;

		Class<?> type = properties.type;
		if (type == byte.class || type == Byte.class) classMinValue = Byte.MIN_VALUE;
		if (type == short.class || type == Short.class) classMinValue = Short.MIN_VALUE;
		if (type == int.class || type == Integer.class) classMinValue = Integer.MIN_VALUE;

		return max(minValue, classMinValue);
	}

	private long getMaxValue() {
		long maxValue = intField.maxValue();

		long classMaxValue = Long.MAX_VALUE;

		Class<?> type = properties.type;
		if (type == byte.class || type == Byte.class) classMaxValue = Byte.MAX_VALUE;
		if (type == short.class || type == Short.class) classMaxValue = Short.MAX_VALUE;
		if (type == int.class || type == Integer.class) classMaxValue = Integer.MAX_VALUE;

		return min(maxValue, classMaxValue);
	}
}
