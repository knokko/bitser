package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.BitserCache;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

abstract class BitFieldWrapper implements Comparable<BitFieldWrapper> {

    protected final BitField bitField;
    protected final Field classField;

    BitFieldWrapper(BitField bitField, Field classField) {
        this.bitField = bitField;
        this.classField = classField;
        if (Modifier.isStatic(classField.getModifiers()) && bitField != null) {
            throw new Error("Static fields should not have BitField annotation: " + classField);
        }
        if (!Modifier.isPublic(classField.getModifiers()) || Modifier.isFinal(classField.getModifiers())) {
            classField.setAccessible(true);
        }
    }

    @Override
    public int compareTo(BitFieldWrapper other) {
        return Integer.compare(this.bitField.ordering(), other.bitField.ordering());
    }

    void write(Object object, BitOutputStream output, BitserCache cache) throws IOException {
        try {
            writeField(object, output, cache);
        } catch (IllegalAccessException shouldNotHappen) {
            throw new Error(shouldNotHappen);
        }
    }

    void writeField(Object object, BitOutputStream output, BitserCache cache) throws IOException, IllegalAccessException {
        writeValue(classField.get(object), output, cache);
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
        classField.set(object, readValue(input, cache));
    }

    abstract Object readValue(BitInputStream input, BitserCache cache) throws IOException, IllegalAccessException;
}
