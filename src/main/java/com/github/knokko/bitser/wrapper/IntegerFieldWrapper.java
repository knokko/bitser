package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.serialize.ReadJob;
import com.github.knokko.bitser.serialize.WriteJob;
import com.github.knokko.bitser.util.VirtualField;

import java.io.IOException;
import java.util.function.Consumer;

import static com.github.knokko.bitser.serialize.IntegerBitser.*;
import static java.lang.Long.max;
import static java.lang.Long.min;

@BitStruct(backwardCompatible = false)
public class IntegerFieldWrapper extends BitFieldWrapper {

	private static long getMinValue(long rawMinValue, Class<?> type) {
		long classMinValue = Long.MIN_VALUE;

		if (type == byte.class || type == Byte.class) classMinValue = Byte.MIN_VALUE;
		if (type == short.class || type == Short.class) classMinValue = Short.MIN_VALUE;
		if (type == int.class || type == Integer.class) classMinValue = Integer.MIN_VALUE;

		return max(rawMinValue, classMinValue);
	}

	private static long getMaxValue(long rawMaxValue, Class<?> type) {
		long classMaxValue = Long.MAX_VALUE;

		if (type == byte.class || type == Byte.class) classMaxValue = Byte.MAX_VALUE;
		if (type == short.class || type == Short.class) classMaxValue = Short.MAX_VALUE;
		if (type == int.class || type == Integer.class) classMaxValue = Integer.MAX_VALUE;

		return min(rawMaxValue, classMaxValue);
	}

	@BitField
	private final IntegerField.Properties intField;

	IntegerFieldWrapper(VirtualField field, IntegerField intField) {
		super(field);
		this.intField = new IntegerField.Properties(
				getMinValue(intField.minValue(), field.type),
				getMaxValue(intField.maxValue(), field.type),
				intField.expectUniform()
		);
	}

	@SuppressWarnings("unused")
	private IntegerFieldWrapper() {
		super();
		this.intField = new IntegerField.Properties();
	}

	@Override
	void writeValue(Object fatValue, WriteJob write) throws IOException {
		long value = fatValue instanceof Character ? (long) ((char) fatValue) : ((Number) fatValue).longValue();
		write.output.prepareProperty("int-value", -1);
		if (intField.expectUniform) encodeUniformInteger(value, intField.minValue, intField.maxValue, write.output);
		else encodeVariableInteger(value, intField.minValue, intField.maxValue, write.output);
		write.output.finishProperty();
	}

	@Override
	void readValue(ReadJob read, ValueConsumer setValue) throws IOException {
		long longValue;
		if (intField.expectUniform) longValue = decodeUniformInteger(intField.minValue, intField.maxValue, read.input);
		else longValue = decodeVariableInteger(intField.minValue, intField.maxValue, read.input);

		Class<?> type = field.type;
		if (type == byte.class || type == Byte.class) setValue.consume((byte) longValue);
		else if (type == short.class || type == Short.class) setValue.consume((short) longValue);
		else if (type == int.class || type == Integer.class) setValue.consume((int) longValue);
		else if (type == long.class || type == Long.class || type == null) setValue.consume(longValue);
		else throw new Error("Unexpected integer type " + type);
	}

	@Override
	void setLegacyValue(ReadJob read, Object value, Consumer<Object> setValue) {
		if (value == null) {
			super.setLegacyValue(read, value, setValue);
		} else if (value instanceof Number || value instanceof Character) {
			long l = value instanceof Number ? ((Number) value).longValue() : (long) ((char) value);
			if (l < intField.minValue || l > intField.maxValue) {
				throw new InvalidBitValueException("Legacy value " + value + " is out of range for field " + field);
			}

			Class<?> type = field.type;
			if (type == byte.class || type == Byte.class) super.setLegacyValue(read, (byte) l, setValue);
			else if (type == short.class || type == Short.class) super.setLegacyValue(read, (short) l, setValue);
			else if (type == char.class || type == Character.class) super.setLegacyValue(read, (char) l, setValue);
			else if (type == int.class || type == Integer.class) super.setLegacyValue(read, (int) l, setValue);
			else super.setLegacyValue(read, l, setValue);
		} else {
			throw new InvalidBitFieldException("Can't convert from legacy " + value + " to " + field.type + " for field " + field);
		}
	}
}
