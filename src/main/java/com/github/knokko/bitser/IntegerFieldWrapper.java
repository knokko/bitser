package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.util.Recursor;

import java.util.function.Consumer;

import static com.github.knokko.bitser.IntegerBitser.*;
import static java.lang.Long.max;
import static java.lang.Long.min;

@BitStruct(backwardCompatible = false)
class IntegerFieldWrapper extends BitFieldWrapper {

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
	void writeValue(Object fatValue, Recursor<WriteContext, WriteInfo> recursor) {
		recursor.runFlat("int-value", context -> {
			long value = fatValue instanceof Character ? (long) ((char) fatValue) : ((Number) fatValue).longValue();
			context.output.prepareProperty("int-value", -1);
			if (intField.expectUniform) encodeUniformInteger(value, intField.minValue, intField.maxValue, context.output);
			else encodeVariableInteger(value, intField.minValue, intField.maxValue, context.output);
			context.output.finishProperty();
		});
	}

	@Override
	void readValue(Recursor<ReadContext, ReadInfo> recursor, Consumer<Object> setValue) {
		recursor.runFlat("int-value", read -> {
			long longValue;
			if (intField.expectUniform) longValue = decodeUniformInteger(intField.minValue, intField.maxValue, read.input);
			else longValue = decodeVariableInteger(intField.minValue, intField.maxValue, read.input);

			Class<?> type = field.type;
			if (type == byte.class || type == Byte.class) setValue.accept((byte) longValue);
			else if (type == short.class || type == Short.class) setValue.accept((short) longValue);
			else if (type == int.class || type == Integer.class) setValue.accept((int) longValue);
			else if (type == long.class || type == Long.class || type == null) setValue.accept(longValue);
			else throw new InvalidBitFieldException("Unexpected integer type " + type);
		});
	}

	@Override
	void setLegacyValue(Recursor<ReadContext, ReadInfo> recursor, Object value, Consumer<Object> setValue) {
		if (value == null) {
			super.setLegacyValue(recursor, value, setValue);
		} else if (value instanceof Number || value instanceof Character) {
			long l = value instanceof Number ? ((Number) value).longValue() : (long) ((char) value);
			if (l < intField.minValue || l > intField.maxValue) {
				throw new InvalidBitValueException("Legacy value " + value + " is out of range for field " + field);
			}

			Class<?> type = field.type;
			if (type == byte.class || type == Byte.class) super.setLegacyValue(recursor, (byte) l, setValue);
			else if (type == short.class || type == Short.class) super.setLegacyValue(recursor, (short) l, setValue);
			else if (type == char.class || type == Character.class) super.setLegacyValue(recursor, (char) l, setValue);
			else if (type == int.class || type == Integer.class) super.setLegacyValue(recursor, (int) l, setValue);
			else super.setLegacyValue(recursor, l, setValue);
		} else {
			throw new InvalidBitFieldException("Can't convert from legacy " + value + " to " + field.type + " for field " + field);
		}
	}
}
