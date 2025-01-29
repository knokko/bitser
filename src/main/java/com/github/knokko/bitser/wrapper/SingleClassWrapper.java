package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.backward.LegacyClass;
import com.github.knokko.bitser.backward.LegacyClasses;
import com.github.knokko.bitser.backward.LegacyField;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.field.*;
import com.github.knokko.bitser.serialize.BitserCache;
import com.github.knokko.bitser.serialize.LabelCollection;
import com.github.knokko.bitser.serialize.ReadJob;
import com.github.knokko.bitser.serialize.WriteJob;
import com.github.knokko.bitser.util.ReferenceIdMapper;
import com.github.knokko.bitser.util.VirtualField;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

import static com.github.knokko.bitser.wrapper.WrapperFactory.createComplexWrapper;

class SingleClassWrapper {

	private static final BitField DEFAULT_BIT_FIELD = new BitField() {

		@Override
		public Class<? extends Annotation> annotationType() {
			return BitField.class;
		}

		@Override
		public int id() {
			return -1;
		}

		@Override
		public boolean optional() {
			return false;
		}
	};

	private final Class<?> myClass;
	final List<FieldWrapper> fields = new ArrayList<>();

	SingleClassWrapper(Class<?> myClass, boolean backwardCompatible) {
		this.myClass = myClass;

		Set<Integer> IDs = new HashSet<>();
		Field[] classFields = myClass.getDeclaredFields();
		for (Field classField : classFields) {
			if (Modifier.isStatic(classField.getModifiers())) continue;
			BitField bitField = classField.getAnnotation(BitField.class);
			if (bitField == null) {
				Class<?>[] otherFields = {
						ClassField.class, FloatField.class, IntegerField.class, NestedFieldSetting.class,
						NestedFieldSettings.class, ReferenceField.class, ReferenceFieldTarget.class,
						StableReferenceFieldId.class, StringField.class
				};

				for (Class<?> otherField : otherFields) {
					//noinspection unchecked
					if (classField.isAnnotationPresent((Class<? extends Annotation>) otherField)) {
						bitField = DEFAULT_BIT_FIELD;
						break;
					}
				}
			}
			if (bitField != null) {
				if (bitField.id() < 0 && backwardCompatible) {
					throw new InvalidBitFieldException("BitField IDs must be non-negative when backward compatible: " + classField);
				}
				if (bitField.id() >= 0) {
					if (IDs.contains(bitField.id())) {
						throw new InvalidBitFieldException(myClass + " has multiple @BitField's with id " + bitField.id());
					}
					IDs.add(bitField.id());
				}
				if (Modifier.isFinal(classField.getModifiers()) || !Modifier.isPublic(classField.getModifiers())) {
					classField.setAccessible(true);
				}
				VirtualField field = new VirtualField(
						classField.toString(),
						classField.getType(),
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

				BitFieldWrapper bitFieldWrapper = createComplexWrapper(
						myClass, field.annotations, field, classField.getGenericType(), "", false
				);
				fields.add(new FieldWrapper(bitField.id(), classField, bitFieldWrapper));
			}
		}

		if (backwardCompatible) fields.sort(Comparator.comparingInt(a -> a.id));
		else fields.sort(Comparator.comparing(a -> a.classField.getName()));
	}

	@Override
	public String toString() {
		return myClass.getName();
	}

	public void collectReferenceTargetLabels(LabelCollection labels) {
		for (FieldWrapper field : fields) {
			field.bitField.collectReferenceTargetLabels(labels);
		}
	}

	void registerReferenceTargets(Object object, BitserCache cache, ReferenceIdMapper mapper) {
		for (FieldWrapper field : fields) {
			field.bitField.registerReferenceTargets(field.bitField.field.getValue.apply(object), cache, mapper);
		}
	}

	LegacyClass register(LegacyClasses legacy) {
		LegacyClass legacyClass = legacy.addClass(myClass);
		if (!legacyClass.fields.isEmpty()) return legacyClass;

		for (FieldWrapper field : fields) {
			legacyClass.fields.add(new LegacyField(field.id, field.bitField));
			field.bitField.registerLegacyClasses(legacy);
		}
		return legacyClass;
	}

	void write(Object object, WriteJob write) throws IOException {
		for (FieldWrapper field : fields) field.bitField.write(object, write);
	}

	void read(Object target, ReadJob read, LegacyClass legacy) throws IOException {
		if (legacy != null) {
			int maxId = -1;
			for (LegacyField field : legacy.fields) {
				if (field.id > maxId) maxId = field.id;
			}
			Object[] legacyProperties = new Object[maxId + 1];

			maxId = -1;
			for (FieldWrapper field : fields) {
				if (field.id > maxId) maxId = field.id;
			}
			FieldWrapper[] newFields = new FieldWrapper[maxId + 1];
			for (FieldWrapper field : fields) newFields[field.id] = field;

			int[] counter = new int[1];
			for (LegacyField field : legacy.fields) {
				System.out.println("Reading legacy field " + field.bitField);
				field.bitField.readField(read, legacyValue -> {
					if (field.id < newFields.length) {
						FieldWrapper newField = newFields[field.id];
						if (newField != null) newField.bitField.setLegacyValue(target, legacyValue);
					}
					legacyProperties[field.id] = legacyValue;
					counter[0] += 1;
					if (counter[0] == legacy.fields.size()) {
						// TODO Call postInit method
						System.out.println("Legacy properties are " + Arrays.toString(legacyProperties));
					}
				});
			}
		} else {
			System.out.println("Reading non-legacy fields");
			for (FieldWrapper field : fields) field.bitField.readField(target, read);
		}
	}

	void shallowCopy(Object original, Object target) {
		for (FieldWrapper fieldWrapper : fields) {
			fieldWrapper.bitField.field.setValue.accept(target, fieldWrapper.bitField.field.getValue.apply(original));
		}
	}

	static class FieldWrapper {

		final int id;
		final Field classField;
		final BitFieldWrapper bitField;

		FieldWrapper(int id, Field classField, BitFieldWrapper bitField) {
			this.id = id;
			this.classField = classField;
			this.bitField = bitField;
		}
	}
}
