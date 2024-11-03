package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.field.*;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.BitserCache;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class BitStructWrapper<T> extends BitserWrapper<T> {

    static BitFieldWrapper createWrapper(BitField bitField, Field classField, Class<?> objectClass) {
        List<BitFieldWrapper> result = new ArrayList<>(1);

        IntegerField intField = classField.getAnnotation(IntegerField.class);
        if (intField != null) result.add(new IntegerFieldWrapper(bitField, intField, classField));

        FloatField floatField = classField.getAnnotation(FloatField.class);
        if (floatField != null) result.add(new FloatFieldWrapper(bitField, floatField, classField));

        StringField stringField = classField.getAnnotation(StringField.class);
        if (stringField != null) result.add(new StringFieldWrapper(bitField, stringField, classField));

        StructField structField = classField.getAnnotation(StructField.class);
        if (structField != null) result.add(new StructFieldWrapper(bitField, structField, classField));

        if (classField.getType() == UUID.class) result.add(new UUIDFieldWrapper(bitField, classField));

        CollectionField collectionField = classField.getAnnotation(CollectionField.class);
        if (collectionField != null) {
            try {
                Field valueField = objectClass.getDeclaredField(collectionField.valueAnnotations());
                if (classField.getType().isArray()) {
                    if (classField.getType().getComponentType() != valueField.getType()) {
                        throw new IllegalArgumentException(valueField + " doesn't have the array type of " + classField);
                    }
                } else {
                    if (classField.getGenericType() instanceof ParameterizedType) {
                        ParameterizedType genericType = (ParameterizedType) classField.getGenericType();
                        if (genericType.getActualTypeArguments().length == 0) {
                            throw new IllegalArgumentException("Missing type argument for " + classField);
                        }
                        if (genericType.getActualTypeArguments().length > 1) {
                            throw new IllegalArgumentException("Too many type arguments for " + classField);
                        }
                        if (genericType.getActualTypeArguments()[0] != valueField.getType()) {
                            throw new IllegalArgumentException(valueField + " doesn't have the generic type of " + classField);
                        }
                    }
                }
                BitFieldWrapper valueWrapper = createWrapper(null, valueField, objectClass);
                return new CollectionFieldWrapper(bitField, classField, collectionField.size(), valueWrapper);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(
                        "valueAnnotations of " + classField + " is " + collectionField.valueAnnotations() +
                                ", but can't find " + objectClass + "." + collectionField.valueAnnotations()
                );
            }
        }

        if (result.isEmpty()) throw new Error("Can't handle field " + objectClass + "." + classField.getName());
        if (result.size() > 1) throw new Error("Too many annotations on " + objectClass + "." + classField.getName());
        return result.get(0);
    }

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
            if (bitField != null) fields.add(createWrapper(bitField, classField, objectClass));
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
