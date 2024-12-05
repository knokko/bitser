package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitEnum;
import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.field.*;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.BitserCache;
import com.github.knokko.bitser.util.VirtualField;
import com.github.knokko.bitser.util.ReferenceIdLoader;
import com.github.knokko.bitser.util.ReferenceIdMapper;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

class BitStructWrapper<T> extends BitserWrapper<T> {

	private static final IntegerField DEFAULT_SIZE_FIELD = new IntegerField() {

		@Override
		public Class<? extends Annotation> annotationType() {
			return IntegerField.class;
		}

		@Override
		public boolean expectUniform() {
			return false;
		}

		@Override
		public long minValue() {
			return 0;
		}

		@Override
		public long maxValue() {
			return Integer.MAX_VALUE;
		}
	};

	private static NestedFieldSetting getSetting(
			VirtualField.AnnotationHolder annotations, String path, VirtualField field
	) {
		NestedFieldSetting parentSettings = null;

		NestedFieldSetting[] nestedSettings = annotations.getMultiple(NestedFieldSetting.class);
		if (nestedSettings != null) {
			for (NestedFieldSetting setting : nestedSettings) {
				if (path.equals(setting.path())) {
					if (parentSettings != null) {
						throw new InvalidBitFieldException("Multiple NestedFieldSetting's for path " + path + " of " + field);
					}
					parentSettings = setting;
				}
			}
		}

		return parentSettings;
	}

	private static VirtualField.AnnotationHolder getSettingAnnotations(
			VirtualField.AnnotationHolder original, NestedFieldSetting setting, Class<?> objectClass
	) {
		if (setting == null || setting.fieldName().isEmpty()) return original;

		try {
			return new VirtualField.FieldAnnotations(objectClass.getDeclaredField(setting.fieldName()));
		} catch (NoSuchFieldException nope) {
			throw new InvalidBitFieldException(nope.getMessage());
		}
	}

	private static BitFieldWrapper createComplexWrapper(
			Class<?> objectClass, VirtualField.AnnotationHolder rootAnnotations, VirtualField field,
			Type genericType, String path, boolean expectNothing
	) {
		if (Collection.class.isAssignableFrom(field.type) || field.type.isArray()) {
			if (rootAnnotations.get(BitField.class).optional()) {
				throw new InvalidBitFieldException("optional BitField is not allowed on collection field " +
						field + ": use @NestedFieldSetting instead");
			}
			NestedFieldSetting parentSettings = getSetting(rootAnnotations, path, field);
			VirtualField.AnnotationHolder parentAnnotations = getSettingAnnotations(
					new VirtualField.NoAnnotations(), parentSettings, objectClass
			);

			ReferenceField referenceField = parentAnnotations.get(ReferenceField.class);
			if (referenceField != null) {
				if (parentAnnotations.has(ReferenceFieldTarget.class)) throw new InvalidBitFieldException(
						field + " is both a reference field and a reference target, which is forbidden"
				);
				if (referenceField.stable()) return new StableReferenceFieldWrapper(field, referenceField.label());
				return new UnstableReferenceFieldWrapper(field, referenceField.label());
			}

			Class<?> childFieldType;
			VirtualField.AnnotationHolder childAnnotations;
			Type childGenericType;
			if (field.type.isArray() && (genericType == null || genericType instanceof Class<?>)) {
				childFieldType = field.type.getComponentType();
				childAnnotations = rootAnnotations;
				childGenericType = null;
			} else {
				Type actualTypeArgument = getActualTypeArgument(field, genericType);

				if (actualTypeArgument instanceof Class<?>) {
					childFieldType = (Class<?>) actualTypeArgument;
					childAnnotations = rootAnnotations;
					childGenericType = null;
				} else if (actualTypeArgument instanceof ParameterizedType) {
					ParameterizedType parType = (ParameterizedType) actualTypeArgument;
					Type rawChildType = parType.getRawType();
					if (rawChildType instanceof Class<?>) {
						childFieldType = (Class<?>) rawChildType;
						childAnnotations = new VirtualField.NoAnnotations();
						childGenericType = parType;
					} else throw new InvalidBitFieldException("Unexpected raw actual type argument for " + field);
				} else if (actualTypeArgument instanceof GenericArrayType) {
					GenericArrayType arrayType = (GenericArrayType) actualTypeArgument;
					Type elementType = arrayType.getGenericComponentType();
					childGenericType = arrayType;
					childAnnotations = new VirtualField.NoAnnotations();
					if (elementType instanceof ParameterizedType) {
						childFieldType = (Class<?>) ((ParameterizedType) elementType).getRawType();
						childFieldType = Array.newInstance(childFieldType, 0).getClass();
					}
					else throw new RuntimeException("element type is " + elementType + " and array type is " + arrayType);
				} else {
					throw new InvalidBitFieldException(
							"Unexpected generic type for " + field + ": " + actualTypeArgument.getClass()
					);
				}
			}

			String childPath = path + "c";

			NestedFieldSetting childSettings = getSetting(rootAnnotations, childPath, field);
			childAnnotations = getSettingAnnotations(childAnnotations, childSettings, objectClass);

			VirtualField childField = new VirtualField(
					"c " + field,
					childFieldType,
					-1,
					childSettings != null && childSettings.optional(),
					childAnnotations,
					null,
					null
			);

			BitFieldWrapper childWrapper = createComplexWrapper(
					objectClass, rootAnnotations, childField, childGenericType, childPath,
					parentSettings != null && parentSettings.writeAsBytes()
			);

			VirtualField parentField = new VirtualField(
					field.toString(),
					field.type,
					field.ordering,
					parentSettings != null && parentSettings.optional(),
					parentAnnotations,
					field.getValue,
					field.setValue
			);
			if (parentSettings != null && parentSettings.writeAsBytes()) {
				if (childWrapper != null) {
					throw new InvalidBitFieldException("Value annotations are forbidden when writeAsBytes is true: " + field);
				}
				if (childSettings != null) {
					throw new InvalidBitFieldException("NestedFieldSetting's on writeAsBytes targets is forbidden: " + field);
				}

				return new ByteCollectionFieldWrapper(parentField, parentSettings.sizeField());
			} else {
				IntegerField sizeField = parentSettings != null ? parentSettings.sizeField() : DEFAULT_SIZE_FIELD;
				return new BitCollectionFieldWrapper(parentField, sizeField, childWrapper);
			}
		}

		return createSimpleWrapper(field, expectNothing);
	}

	private static Type getActualTypeArgument(VirtualField field, Type genericType) {
		Type actualTypeArgument;
		if (genericType instanceof ParameterizedType) {
			ParameterizedType parGenericType = (ParameterizedType) genericType;

			Type[] actualTypeArguments = parGenericType.getActualTypeArguments();
			if (actualTypeArguments.length == 0) {
				throw new Error("Missing type argument for " + field);
			}
			if (actualTypeArguments.length > 1) {
				throw new InvalidBitFieldException("Too many generic types for " + field);
			}

			actualTypeArgument = actualTypeArguments[0];
		} else if (genericType instanceof GenericArrayType) {
			GenericArrayType genericArrayType = (GenericArrayType) genericType;
			actualTypeArgument = genericArrayType.getGenericComponentType();
		} else throw new InvalidBitFieldException("Unexpected generic type " + genericType + " for " + field);
		return actualTypeArgument;
	}


	private static BitFieldWrapper createSimpleWrapper(VirtualField field, boolean expectNothing) {
		if (field.referenceTargetLabel != null && field.type.isPrimitive()) {
			throw new InvalidBitFieldException("Reference target " + field + " is primitive, which is forbidden");
		}

		if (field.annotations.has(StableReferenceFieldId.class)) {
			if (field.type != UUID.class) throw new InvalidBitFieldException(
					"Only UUID fields can have @StableReferenceFieldId"
			);
			if (field.optional) throw new InvalidBitFieldException("@StableReferenceFieldId's can't be optional");
		}

		ReferenceField referenceField = field.annotations.get(ReferenceField.class);
		if (referenceField != null) {
			if (field.annotations.has(ReferenceFieldTarget.class)) throw new InvalidBitFieldException(
					field + " is both a reference field and a reference target, which is forbidden"
			);
			if (referenceField.stable()) return new StableReferenceFieldWrapper(field, referenceField.label());
			return new UnstableReferenceFieldWrapper(field, referenceField.label());
		}

		List<BitFieldWrapper> result = new ArrayList<>(1);

		IntegerField intField = field.annotations.get(IntegerField.class);
		if (intField != null) result.add(new IntegerFieldWrapper(field, intField));

		FloatField floatField = field.annotations.get(FloatField.class);
		if (floatField != null) result.add(new FloatFieldWrapper(field, floatField));

		StringField stringField = field.annotations.get(StringField.class);
		if (stringField != null) result.add(new StringFieldWrapper(field, stringField));

		if (field.type.getAnnotation(BitStruct.class) != null) result.add(new StructFieldWrapper(field));

		BitEnum bitEnum = field.type.getAnnotation(BitEnum.class);
		if (bitEnum != null) result.add(new EnumFieldWrapper(field, bitEnum));

		if (expectNothing && result.isEmpty()) return null;

		if (field.type == UUID.class) result.add(new UUIDFieldWrapper(field));

		if (field.type == boolean.class || field.type == Boolean.class) result.add(new BooleanFieldWrapper(field));
		if (field.type == String.class && stringField == null) result.add(new StringFieldWrapper(field, null));

		if (result.isEmpty()) throw new InvalidBitFieldException("Missing annotations for " + field);
		if (result.size() > 1) throw new Error("Too many annotations on " + field);
		return result.get(0);
	}

	private final BitStruct bitStruct;
	private final List<BitFieldWrapper> fields = new ArrayList<>();
	private final Constructor<T> constructor;
	private final VirtualField stableIdField;

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
				if (Modifier.isStatic(classField.getModifiers())) {
					throw new InvalidBitFieldException("@BitField is not allowed on static fields");
				}
				if (bitField.ordering() < 0) throw new InvalidBitFieldException("ordering must be non-negative");
				if (Modifier.isFinal(classField.getModifiers()) || !Modifier.isPublic(classField.getModifiers())) {
					classField.setAccessible(true);
				}
				VirtualField field = new VirtualField(
						classField.toString(),
						classField.getType(),
						bitField.ordering() + orderingOffsets[index],
						bitField.optional(),
						new VirtualField.FieldAnnotations(classField),
						target -> {
							try {
								return classField.get(target);
							} catch (IllegalAccessException e) {
								throw new Error(e);
							}
						},
						(target, newValue) -> {
							try {
								classField.set(target, newValue);
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							}
						}
				);
				fields.add(createComplexWrapper(
						objectClass, field.annotations, field, classField.getGenericType(), "", false
				));
			}
		}

		fields.sort(null);

		VirtualField stableIdField = null;

		for (int index = 0; index < fields.size(); index++) {
			BitFieldWrapper field = fields.get(index);
			if (field.field.ordering != index) {
				throw new InvalidBitFieldException("Orderings of " + objectClass + " has gaps");
			}
			if (field instanceof UUIDFieldWrapper && ((UUIDFieldWrapper) field).isStableReferenceId) {
				if (stableIdField != null) throw new InvalidBitFieldException(
						"Bit struct " + objectClass + " has multiple stable ID fields, but at most 1 is allowed"
				);
				stableIdField = field.field;
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
			field.registerReferenceTargets(field.field.getValue.apply(object), cache, mapper);
		}
	}

	@Override
	public UUID getStableId(Object target) {
		if (stableIdField == null) throw new InvalidBitFieldException(target + " doesn't have an @StableReferenceFieldId");
		return (UUID) stableIdField.getValue.apply(target);
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
