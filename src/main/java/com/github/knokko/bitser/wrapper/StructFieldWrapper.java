package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.StructField;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.BitserCache;

import java.io.IOException;
import java.lang.reflect.Field;

class StructFieldWrapper extends BitFieldWrapper {

    private final StructField structField;

    StructFieldWrapper(BitField bitField, StructField structField, Field classField) {
        super(bitField, classField);
        this.structField = structField;
    }

    @Override
    void writeField(Object object, BitOutputStream output, BitserCache cache) throws IOException, IllegalAccessException {
        Object value = classField.get(object);
        if (structField.nullable()) output.write(value != null);
        else if (value == null) throw new Error("value can't be null"); // TODO Create proper exception for this

        if (value != null) cache.getWrapper(classField.getType()).write(value, output, cache);
    }

    @Override
    void readField(Object object, BitInputStream input, BitserCache cache) throws IOException, IllegalAccessException {
        Object value;
        if (structField.nullable() && !input.read()) value = null;
        else value = cache.getWrapper(classField.getType()).read(input, cache);

        classField.set(object, value);
    }
}
