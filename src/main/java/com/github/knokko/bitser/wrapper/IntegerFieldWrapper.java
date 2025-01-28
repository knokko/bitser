package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.serialize.ReadJob;
import com.github.knokko.bitser.serialize.WriteJob;
import com.github.knokko.bitser.util.VirtualField;

import java.io.IOException;

import static com.github.knokko.bitser.serialize.IntegerBitser.*;
import static java.lang.Long.max;
import static java.lang.Long.min;

@BitStruct(backwardCompatible = false)
public class IntegerFieldWrapper extends BitFieldWrapper {

	@BitField
	private final IntegerField.Properties intField;

	IntegerFieldWrapper(VirtualField field, IntegerField intField) {
		super(field);
		this.intField = new IntegerField.Properties(intField);
	}

	@Override
	void writeValue(Object fatValue, WriteJob write) throws IOException {
		long value = ((Number) fatValue).longValue();
		if (intField.expectUniform) encodeUniformInteger(value, getMinValue(), getMaxValue(), write.output);
		else encodeVariableInteger(value, getMinValue(), getMaxValue(), write.output);
	}

	@Override
	void readValue(ReadJob read, ValueConsumer setValue) throws IOException {
		long longValue;
		if (intField.expectUniform) longValue = decodeUniformInteger(getMinValue(), getMaxValue(), read.input);
		else longValue = decodeVariableInteger(getMinValue(), getMaxValue(), read.input);

		Class<?> type = field.type;
		if (type == byte.class || type == Byte.class) setValue.consume((byte) longValue);
		else if (type == short.class || type == Short.class) setValue.consume((short) longValue);
		else if (type == int.class || type == Integer.class) setValue.consume((int) longValue);
		else setValue.consume(longValue);
	}

	private long getMinValue() {
		long minValue = intField.minValue;

		long classMinValue = Long.MIN_VALUE;

		Class<?> type = field.type;
		if (type == byte.class || type == Byte.class) classMinValue = Byte.MIN_VALUE;
		if (type == short.class || type == Short.class) classMinValue = Short.MIN_VALUE;
		if (type == int.class || type == Integer.class) classMinValue = Integer.MIN_VALUE;

		return max(minValue, classMinValue);
	}

	private long getMaxValue() {
		long maxValue = intField.maxValue;

		long classMaxValue = Long.MAX_VALUE;

		Class<?> type = field.type;
		if (type == byte.class || type == Byte.class) classMaxValue = Byte.MAX_VALUE;
		if (type == short.class || type == Short.class) classMaxValue = Short.MAX_VALUE;
		if (type == int.class || type == Integer.class) classMaxValue = Integer.MAX_VALUE;

		return min(maxValue, classMaxValue);
	}
}
