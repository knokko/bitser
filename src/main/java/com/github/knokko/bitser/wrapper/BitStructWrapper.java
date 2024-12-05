package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitEnum;
import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.field.*;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.BitserCache;
import com.github.knokko.bitser.util.ReferenceIdLoader;
import com.github.knokko.bitser.util.ReferenceIdMapper;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;

class BitStructWrapper<T> extends BitserWrapper<T> {

	static BitFieldWrapper createWrapper(
			BitField.Properties properties, Field classField, Class<?> objectClass,
			int depth, boolean expectNothing
	) {
		if (properties.referenceTarget != null && classField.getType().isPrimitive()) {
			throw new InvalidBitFieldException("Reference target " + classField + " is primitive, which is forbidden");
		}
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
			BitField.Properties valueProperties = new BitField.Properties(
					-1, collectionField.optionalValues(), innerFieldType, properties.referenceTarget
			);
			BitFieldWrapper valueWrapper = createWrapper(
					valueProperties, classField, objectClass, depth + 1, collectionField.writeAsBytes()
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
				if (properties.referenceTarget != null) properties = new BitField.Properties(
						properties.ordering, properties.optional, properties.type, null
				);
				return new BitCollectionFieldWrapper(properties, classField, collectionField.size(), valueWrapper);
			}
		}

		if (classField.isAnnotationPresent(StableReferenceFieldId.class)) {
			if (properties.type != UUID.class) throw new InvalidBitFieldException(
					"Only UUID fields can have @StableReferenceFieldId"
			);
			if (properties.optional) throw new InvalidBitFieldException("@StableReferenceFieldId's can't be optional");
		}

		ReferenceField referenceField = classField.getAnnotation(ReferenceField.class);
		if (referenceField != null) {
			if (classField.isAnnotationPresent(ReferenceFieldTarget.class)) throw new InvalidBitFieldException(
					classField + " is both a reference field and a reference target, which is forbidden"
			);
			if (referenceField.stable()) return new StableReferenceFieldWrapper(properties, classField, referenceField.label());
			return new UnstableReferenceFieldWrapper(properties, classField, referenceField.label());
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

		if (properties.type == UUID.class) {
			result.add(new UUIDFieldWrapper(
					properties, classField, classField.isAnnotationPresent(StableReferenceFieldId.class))
			);
		}

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
	private final Field stableIdField;

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
						bitField.ordering() + orderingOffsets[index],
						bitField.optional(),
						classField.getType(),
						classField.getAnnotation(ReferenceFieldTarget.class)
				), classField, objectClass, 0, false));
			}
		}

		fields.sort(null);

		Field stableIdField = null;

		for (int index = 0; index < fields.size(); index++) {
			BitFieldWrapper field = fields.get(index);
			if (field.properties.ordering != index) {
				throw new InvalidBitFieldException("Orderings of " + objectClass + " has gaps");
			}
			if (field instanceof UUIDFieldWrapper && ((UUIDFieldWrapper) field).isStableReferenceId) {
				if (stableIdField != null) throw new InvalidBitFieldException(
						"Bit struct " + objectClass + " has multiple stable ID fields, but at most 1 is allowed"
				);
				stableIdField = field.classField;
			}
		}

		this.stableIdField = stableIdField;
	}

	@Override
	public void collectReferenceTargetLabels(
			BitserCache cache, Set<String> declaredTargetLabels,
			Set<String> stableLabels, Set<String> unstableLabels, Set<Object> visitedStructs
	) {
		if (visitedStructs.contains(this)) return;
		visitedStructs.add(this);
		for (BitFieldWrapper field : fields) {
			field.collectReferenceTargetLabels(cache, declaredTargetLabels, stableLabels, unstableLabels, visitedStructs);
		}
	}

	@Override
	public void registerReferenceTargets(Object object, BitserCache cache, ReferenceIdMapper mapper) {
		for (BitFieldWrapper field : fields) {
			try {
				field.registerReferenceTargets(field.classField.get(object), cache, mapper);
			} catch (IllegalAccessException shouldNotHappen) {
				throw new Error(shouldNotHappen);
			}
		}
	}

	@Override
	public UUID getStableId(Object target) {
		if (stableIdField == null) throw new InvalidBitFieldException(target + " doesn't have an @StableReferenceFieldId");
		try {
			return (UUID) stableIdField.get(target);
		} catch (IllegalAccessException shouldNotHappen) {
			throw new Error(shouldNotHappen);
		}
	}

	@Override
	public void write(Object object, BitOutputStream output, BitserCache cache, ReferenceIdMapper idMapper) throws IOException {
		if (bitStruct.backwardCompatible()) throw new UnsupportedOperationException("TODO");
		for (BitFieldWrapper field : fields) field.write(object, output, cache, idMapper);
	}

	@Override
	public void read(
			BitInputStream input, BitserCache cache, ReferenceIdLoader idLoader, ValueConsumer setValue
	) throws IOException {
		if (bitStruct.backwardCompatible()) throw new UnsupportedOperationException("TODO");

		try {
			T object = constructor.newInstance();
			for (BitFieldWrapper field : fields) field.read(object, input, cache, idLoader);
			setValue.consume(object);
		} catch (InstantiationException e) {
			throw new Error("Failed to instantiate " + constructor, e);
		} catch (IllegalAccessException shouldNotHappen) {
			throw new Error(shouldNotHappen);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
}
