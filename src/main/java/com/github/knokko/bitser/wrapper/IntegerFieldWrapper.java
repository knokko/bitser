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
    void writeField(Object object, BitOutputStream output, BitserCache cache) throws IOException, IllegalAccessException {
        long value = classField.getLong(object);
        if (intField.expectUniform()) encodeUniformInteger(value, getMinValue(), getMaxValue(), output);
        else encodeVariableInteger(value, getMinValue(), getMaxValue(), output);
    }

    @Override
    void readField(Object object, BitInputStream input, BitserCache cache) throws IOException, IllegalAccessException {
        long value;
        if (intField.expectUniform()) value = decodeUniformInteger(getMinValue(), getMaxValue(), input);
        else value = decodeVariableInteger(getMinValue(), getMaxValue(), input);

        if (classField.getType() == byte.class) classField.setByte(object, (byte) value);
        else if (classField.getType() == short.class) classField.setShort(object, (short) value);
        else if (classField.getType() == int.class) classField.setInt(object, (int) value);
        else classField.setLong(object, value);
    }

    private long getMinValue() {
        long minValue = intField.minValue();

        long classMinValue = Long.MIN_VALUE;
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
