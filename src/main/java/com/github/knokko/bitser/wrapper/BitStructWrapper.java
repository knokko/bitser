package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitEnum;
import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.field.*;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.BitserCache;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

class BitStructWrapper<T> extends BitserWrapper<T> {

	static BitFieldWrapper createWrapper(
			BitField.Properties properties, Field classField, Class<?> objectClass,
			int depth, boolean expectNothing
	) {
		CollectionField collectionField = classField.getAnnotation(CollectionField.class);
		if (collectionField != null && depth == 0) { // TODO Nested collection fields
			Class<?> innerFieldType;
			if (classField.getType().isArray()) {
				innerFieldType = classField.getType().getComponentType();
			} else {
				if (classField.getGenericType() instanceof ParameterizedType) {
					ParameterizedType genericType = (ParameterizedType) classField.getGenericType();
					if (genericType.getActualTypeArguments().length == 0) {
						throw new Error("Missing type argument for " + classField);
					}
					if (genericType.getActualTypeArguments().length > 1) {
						throw new InvalidBitFieldException("Too many generic types for " + classField);
					}
					if (genericType.getActualTypeArguments()[0].getClass() != Class.class) {
						throw new InvalidBitFieldException("Unexpected generic type for " + classField);
					}
					innerFieldType = (Class<?>) genericType.getActualTypeArguments()[0];
				} else throw new InvalidBitFieldException("Unexpected generic type for " + classField);
			}
			BitFieldWrapper valueWrapper = createWrapper(
					new BitField.Properties(-1, collectionField.optionalValues(), innerFieldType),
					classField, objectClass, depth + 1, collectionField.writeAsBytes()
			);

			if (collectionField.writeAsBytes()) {
				if (collectionField.optionalValues()) {
					throw new InvalidBitFieldException("optionalValues must be false when writeAsBytes is true: " + classField);
				}
				if (valueWrapper != null) {
					throw new InvalidBitFieldException("Value annotations are forbidden when writeAsBytes is true: " + classField);
				}
				return new ByteCollectionFieldWrapper(properties, collectionField.size(), classField);
			} else {
				return new BitCollectionFieldWrapper(properties, classField, collectionField.size(), valueWrapper);
			}
		}

		List<BitFieldWrapper> result = new ArrayList<>(1);

		IntegerField intField = classField.getAnnotation(IntegerField.class);
		if (intField != null) result.add(new IntegerFieldWrapper(properties, intField, classField));

		FloatField floatField = classField.getAnnotation(FloatField.class);
		if (floatField != null) result.add(new FloatFieldWrapper(properties, floatField, classField));

		StringField stringField = classField.getAnnotation(StringField.class);
		if (stringField != null) result.add(new StringFieldWrapper(properties, stringField, classField));

		if (properties.type.getAnnotation(BitStruct.class) != null) {
			result.add(new StructFieldWrapper(properties, classField));
		}

		BitEnum bitEnum = properties.type.getAnnotation(BitEnum.class);
		if (bitEnum != null) result.add(new EnumFieldWrapper(properties, classField, bitEnum, properties.type));

		if (expectNothing && result.isEmpty()) return null;

		if (properties.type == UUID.class) result.add(new UUIDFieldWrapper(properties, classField));
		if (properties.type == boolean.class || properties.type == Boolean.class) {
			result.add(new BooleanFieldWrapper(properties, classField));
		}
		if (properties.type == String.class && stringField == null) {
			result.add(new StringFieldWrapper(properties, null, classField));
		}

		if (result.isEmpty()) throw new InvalidBitFieldException("Missing annotations for " + classField);
		if (result.size() > 1) throw new Error("Too many annotations on " + objectClass + "." + classField.getName());
		return result.get(0);
	}

	private final BitStruct bitStruct;
	private final List<BitFieldWrapper> fields = new ArrayList<>();
	private final Constructor<T> constructor;

	BitStructWrapper(Class<T> objectClass, BitStruct bitStruct) {
		if (bitStruct == null)
			throw new IllegalArgumentException("Class must have a BitStruct annotation: " + objectClass);
		this.bitStruct = bitStruct;

		if (Modifier.isAbstract(objectClass.getModifiers()))
			throw new IllegalArgumentException(objectClass + " is abstract");
		if (Modifier.isInterface(objectClass.getModifiers()))
			throw new IllegalArgumentException(objectClass + " is an interface");

		try {
			this.constructor = objectClass.getDeclaredConstructor();
			if (!Modifier.isPublic(constructor.getModifiers())) constructor.setAccessible(true);
		} catch (NoSuchMethodException e) {
			throw new Error(objectClass + " must have a constructor without parameters");
		}

		Field[] classFields = objectClass.getDeclaredFields();
		int[] orderingOffsets = new int[classFields.length];
		Class<?> superClass = objectClass.getSuperclass();
		while (superClass != null) {
			Field[] superFields = superClass.getDeclaredFields();
			int superOffset = 0;
			for (Field field : superFields) {
				if (field.getAnnotation(BitField.class) != null) superOffset += 1;
			}
			Field[] combinedFields = Arrays.copyOf(superFields, classFields.length + superFields.length);
			int[] combinedOffsets = new int[combinedFields.length];
			for (int index = 0; index < orderingOffsets.length; index++) {
				combinedOffsets[index + superFields.length] = orderingOffsets[index] + superOffset;
			}
			System.arraycopy(classFields, 0, combinedFields, superFields.length, classFields.length);
			classFields = combinedFields;
			orderingOffsets = combinedOffsets;
			superClass = superClass.getSuperclass();
		}

		for (int index = 0; index < classFields.length; index++) {
			Field classField = classFields[index];
			BitField bitField = classField.getAnnotation(BitField.class);
			if (bitField != null) {
				if (bitField.ordering() < 0) throw new InvalidBitFieldException("ordering must be non-negative");
				fields.add(createWrapper(new BitField.Properties(
						bitField.ordering() + orderingOffsets[index], bitField.optional(), classField.getType()
				), classField, objectClass, 0, false));
			}
		}

		fields.sort(null);

		for (int index = 0; index < fields.size(); index++) {
			if (fields.get(index).properties.ordering != index)
				throw new Error("Orderings of " + objectClass + " has gaps");
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
