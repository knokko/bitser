package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.field.*;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.BitserCache;
import com.github.knokko.bitser.util.ReferenceIdLoader;
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
	private final boolean backwardCompatible;
	final List<FieldWrapper> fields = new ArrayList<>();

	SingleClassWrapper(Class<?> myClass, boolean backwardCompatible) {
		this.myClass = myClass;
		this.backwardCompatible = backwardCompatible;

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

	void collectReferenceTargetLabels(
			BitserCache cache, Set<String> declaredTargetLabels,
			Set<String> stableLabels, Set<String> unstableLabels, Set<Object> visitedStructs
	) {
		for (FieldWrapper field : fields) {
			field.bitField.collectReferenceTargetLabels(cache, declaredTargetLabels, stableLabels, unstableLabels, visitedStructs);
		}
	}

	void registerReferenceTargets(Object object, BitserCache cache, ReferenceIdMapper mapper) {
		for (FieldWrapper field : fields) {
			field.bitField.registerReferenceTargets(field.bitField.field.getValue.apply(object), cache, mapper);
		}
	}

	void write(
			Object object, BitOutputStream output, BitserCache cache,
			ReferenceIdMapper idMapper, boolean backwardCompatible
	) throws IOException {
		if (backwardCompatible) throw new UnsupportedOperationException("TODO");
		for (FieldWrapper field : fields) field.bitField.write(object, output, cache, idMapper);
	}

	void read(
			Object target, BitInputStream input, BitserCache cache,
			ReferenceIdLoader idLoader, boolean backwardCompatible
	) throws IOException {
		if (backwardCompatible) throw new UnsupportedOperationException("TODO");
		for (FieldWrapper field : fields) field.bitField.readField(target, input, cache, idLoader);
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
