package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.BitserCache;

import java.io.IOException;
import java.lang.reflect.Field;

import static com.github.knokko.bitser.serialize.IntegerBitser.*;
import static java.lang.Long.max;
import static java.lang.Long.min;

public class IntegerFieldWrapper extends BitFieldWrapper {

    private final IntegerField intField;

    IntegerFieldWrapper(BitField bitField, IntegerField intField, Field classField) {
        super(bitField, classField);
        this.intField = intField;
    }

    @Override
    void writeValue(Object fatValue, BitOutputStream output, BitserCache cache) throws IOException {
        long value = ((Number) fatValue).longValue();
        if (intField.expectUniform()) encodeUniformInteger(value, getMinValue(), getMaxValue(), output);
        else encodeVariableInteger(value, getMinValue(), getMaxValue(), output);
    }

    @Override
    Object readValue(BitInputStream input, BitserCache cache) throws IOException {
        long longValue;
        if (intField.expectUniform()) longValue = decodeUniformInteger(getMinValue(), getMaxValue(), input);
        else longValue = decodeVariableInteger(getMinValue(), getMaxValue(), input);

        Class<?> type = classField.getType();
        if (type == byte.class || type == Byte.class) return (byte) longValue;
        if (type == short.class || type == Short.class) return (short) longValue;
        if (type == int.class || type == Integer.class) return (int) longValue;
        return longValue;
    }

    private long getMinValue() {
        long minValue = intField.minValue();

        long classMinValue = Long.MIN_VALUE;
        // TODO Make this fail
        if (classField.getType() == byte.class) classMinValue = Byte.MIN_VALUE;
        if (classField.getType() == short.class) classMinValue = Short.MIN_VALUE;
        if (classField.getType() == int.class) classMinValue = Integer.MIN_VALUE;

        return max(minValue, classMinValue);
    }

    private long getMaxValue() {
        long maxValue = intField.maxValue();

        long classMaxValue = Long.MAX_VALUE;
        if (classField.getType() == byte.class) classMaxValue = Byte.MAX_VALUE;
        if (classField.getType() == short.class) classMaxValue = Short.MAX_VALUE;
        if (classField.getType() == int.class) classMaxValue = Integer.MAX_VALUE;

        return min(maxValue, classMaxValue);
    }
}
