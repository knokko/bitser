package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.field.StructField;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.BitserCache;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

class BitStructWrapper<T> extends BitserWrapper<T> {

    private final BitStruct bitStruct;
    private final List<BitFieldWrapper> fields = new ArrayList<>();
    private final Constructor<T> constructor;

    BitStructWrapper(Class<T> objectClass, BitStruct bitStruct) {
        if (bitStruct == null) throw new IllegalArgumentException("Class must have a BitStruct annotation: " + objectClass);
        this.bitStruct = bitStruct;

        if (Modifier.isAbstract(objectClass.getModifiers())) throw new IllegalArgumentException(objectClass + " is abstract");
        if (Modifier.isInterface(objectClass.getModifiers())) throw new IllegalArgumentException(objectClass + " is an interface");

        try {
            this.constructor = objectClass.getDeclaredConstructor();
            if (!Modifier.isPublic(constructor.getModifiers())) constructor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new Error(objectClass + " must have a constructor without parameters");
        }

        Field[] classFields = objectClass.getDeclaredFields();
        for (Field classField : classFields) {
            BitField bitField = classField.getAnnotation(BitField.class);
            if (bitField != null) {
                int oldSize = fields.size();

                IntegerField intField = classField.getAnnotation(IntegerField.class);
                if (intField != null) fields.add(new IntegerFieldWrapper(bitField, intField, classField));

                StructField structField = classField.getAnnotation(StructField.class);
                if (structField != null) fields.add(new StructFieldWrapper(bitField, structField, classField));

                if (oldSize == fields.size()) throw new Error("Can't handle field " + objectClass + "." + classField.getName());
                if (oldSize + 1 < fields.size()) throw new Error("Too many annotations on " + objectClass + "." + classField.getName());
            }
        }

        fields.sort(null);

        for (int index = 0; index < fields.size(); index++) {
            if (fields.get(index).bitField.ordering() != index) throw new Error("Orderings of " + objectClass + " has gaps");
        }
    }

    @Override
    public void write(Object object, BitOutputStream output, BitserCache cache) throws IOException {
        if (bitStruct.backwardCompatible()) throw new UnsupportedOperationException("TODO");
        for (BitFieldWrapper field : fields) field.write(object, output, cache);
    }

    @Override
    public T read(BitInputStream input, BitserCache cache) throws IOException {
        if (bitStruct.backwardCompatible()) throw new UnsupportedOperationException("TODO");

        try {
            T object = constructor.newInstance();
            for (BitFieldWrapper field : fields) field.read(object, input, cache);
            return object;
        } catch (InstantiationException e) {
            throw new Error("Failed to instantiate " + constructor, e);
        } catch (IllegalAccessException shouldNotHappen) {
            throw new Error(shouldNotHappen);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
