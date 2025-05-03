package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.backward.*;
import com.github.knokko.bitser.backward.instance.LegacyValues;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.field.*;
import com.github.knokko.bitser.serialize.BitserCache;
import com.github.knokko.bitser.serialize.LabelCollection;
import com.github.knokko.bitser.serialize.ReadJob;
import com.github.knokko.bitser.serialize.WriteJob;
import com.github.knokko.bitser.util.ReferenceIdMapper;
import com.github.knokko.bitser.util.VirtualField;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Supplier;

import static com.github.knokko.bitser.wrapper.WrapperFactory.createComplexWrapper;
import static java.lang.Math.max;

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

	final Class<?> myClass;
	final List<FieldWrapper> fields = new ArrayList<>();
	final List<FieldWrapper> fieldsSortedById;
	final List<FunctionWrapper> functions = new ArrayList<>();

	SingleClassWrapper(Class<?> myClass, boolean backwardCompatible) {
		this.myClass = myClass;

		Class<?>[] otherFields = {
				ClassField.class, FloatField.class, IntegerField.class, NestedFieldSetting.class,
				NestedFieldSettings.class, ReferenceField.class, ReferenceFieldTarget.class,
				StableReferenceFieldId.class, StringField.class, EnumField.class
		};

		Set<Integer> IDs = new HashSet<>();
		for (Field classField : myClass.getDeclaredFields()) {
			if (Modifier.isStatic(classField.getModifiers())) continue;
			BitField bitField = classField.getAnnotation(BitField.class);
			if (bitField == null) {


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

		fields.sort(Comparator.comparing(a -> a.classField.getName()));
		this.fieldsSortedById = new ArrayList<>(fields);
		fieldsSortedById.sort(Comparator.comparingInt(a -> a.id));

		IDs.clear();
		for (Method classMethod : myClass.getDeclaredMethods()) {
			BitField bitField = classMethod.getAnnotation(BitField.class);
			if (bitField == null) continue;
			if (Modifier.isStatic(classMethod.getModifiers())) {
				throw new InvalidBitFieldException("BitField methods must not be static: " + classMethod);
			}
			Parameter[] parameters = classMethod.getParameters();
			if (parameters.length > 1) {
				throw new InvalidBitFieldException("BitField methods can have at most 1 parameter: " + classMethod);
			}
			if (parameters.length == 1 && parameters[0].getType() != FunctionContext.class) {
				throw new InvalidBitFieldException("BitField method parameter type must be FunctionContext: " + classMethod);
			}
			if (bitField.id() < 0) {
				throw new InvalidBitFieldException("BitField method IDs must be non-negative: " + classMethod);
			}
			if (IDs.contains(bitField.id())) {
				throw new InvalidBitFieldException(myClass + " has multiple @BitField methods with id " + bitField.id());
			}
			IDs.add(bitField.id());
			if (!Modifier.isPublic(classMethod.getModifiers())) classMethod.setAccessible(true);
			VirtualField field = new VirtualField(
					classMethod.toString(),
					classMethod.getReturnType(),
					bitField.optional(),
					new VirtualField.MethodAnnotations(classMethod),
					null, null
			);

			BitFieldWrapper bitFieldWrapper = createComplexWrapper(
					myClass, field.annotations, field, classMethod.getGenericReturnType(), "", false
			);
			functions.add(new FunctionWrapper(bitField.id(), classMethod, bitFieldWrapper));
		}

		functions.sort(Comparator.comparingInt(a -> a.id));
	}

	private List<FieldWrapper> getFields(boolean backwardCompatible) {
		return backwardCompatible ? fieldsSortedById : fields;
	}

	@Override
	public String toString() {
		return myClass.getName();
	}

	public void collectReferenceLabels(LabelCollection labels) {
		for (FieldWrapper field : getFields(labels.backwardCompatible)) {
			field.bitField.collectReferenceLabels(labels);
		}
		for (FunctionWrapper function : functions) {
			function.bitField.collectReferenceLabels(labels);
		}
	}

	void registerReferenceTargets(Object object, BitserCache cache, ReferenceIdMapper mapper) {
		for (FieldWrapper field : fields) {
			field.bitField.registerReferenceTargets(field.bitField.field.getValue.apply(object), cache, mapper);
		}
	}

	LegacyClass register(Object object, LegacyClasses legacy) {
		LegacyClass legacyClass = legacy.addClass(myClass);

		for (int index = 0; index < fieldsSortedById.size(); index++) {
			FieldWrapper field = fieldsSortedById.get(index);
			if (legacyClass.fields.size() == index) {
				legacyClass.fields.add(new LegacyField(field.id, field.bitField));
			}
			field.bitField.registerLegacyClasses(field.bitField.field.getValue.apply(object), legacy);
		}

		for (int index = 0; index < functions.size(); index++) {
			FunctionWrapper function = functions.get(index);
			if (legacyClass.functions.size() == index) {
				legacyClass.functions.add(new LegacyField(function.id, function.bitField));
			}
			function.bitField.registerLegacyClasses(function.computeValue(object, legacy.functionContext), legacy);
		}

		return legacyClass;
	}

	void write(Object object, WriteJob write) throws IOException {
		for (FieldWrapper field : getFields(write.legacy != null)) {
			field.bitField.write(object, write);
		}
		FunctionContext functionContext = new FunctionContext(write.bitser, write.withParameters);
		for (FunctionWrapper function : functions) {
			function.bitField.writeValue(function.computeValue(object, functionContext), write);
		}
	}

	void setLegacyValues(ReadJob read, Object target, LegacyValues legacy) {
		int maxFieldId = -1;
		for (FieldWrapper field : fields) maxFieldId = max(maxFieldId, field.id);
		for (FieldWrapper field : fieldsSortedById) {
			if (field.id < legacy.values.length && legacy.hadValues[field.id] &&
					legacy.hadReferenceValues[field.id] == field.bitField.isReference()
			) {
				field.bitField.setLegacyValue(read, legacy.values[field.id], newValue ->
						field.bitField.field.setValue.accept(target, newValue)
				);
			}
		}
		int maxFunctionId = -1;
		for (FunctionWrapper function : functions) maxFunctionId = max(maxFunctionId, function.id);
		legacy.convertedFunctionValues = new Object[max(maxFunctionId + 1, legacy.storedFunctionValues.length)];
		for (FunctionWrapper function : functions) {
			if (legacy.hadFunctionValues.length > function.id && legacy.hadFunctionValues[function.id]) {
				function.bitField.setLegacyValue(
						read, legacy.storedFunctionValues[function.id],
						newValue -> legacy.convertedFunctionValues[function.id] = newValue
				);
			}
		}
	}

	Object[] read(Object target, ReadJob read) throws IOException {
		Object[] functionValues;
		if (functions.isEmpty()) functionValues = new Object[0];
		else functionValues = new Object[functions.get(functions.size() - 1).id + 1];

		for (FieldWrapper field : getFields(read.backwardCompatible)) field.bitField.readField(target, read);
		for (FunctionWrapper function : functions) {
			function.bitField.readValue(read, result -> functionValues[function.id] = result);
		}
		return functionValues;
	}

	private void checkReferenceMigration(
			boolean wasReference, BitFieldWrapper bitField,
			Object value, Supplier<String> fieldDescription
	) {
		if (!bitField.field.optional) {
			if (wasReference && !bitField.isReference()) {
				throw new InvalidBitValueException(
						"Can't store legacy reference in non-reference field " + fieldDescription.get()
				);
			}
			if (!wasReference && bitField.isReference()) {
				throw new InvalidBitValueException(
						"Can't store legacy non-reference " + value + " in " + fieldDescription.get()
				);
			}
		}
	}

	public void fixLegacyTypes(ReadJob read, LegacyValues legacyValues) {
		for (FieldWrapper field : fields) {
			if (field.id >= legacyValues.values.length) continue;
			checkReferenceMigration(
					legacyValues.hadReferenceValues[field.id], field.bitField,
					legacyValues.values[field.id], field.classField::toString
			);
			field.bitField.fixLegacyTypes(read, legacyValues.values[field.id]);
		}
		for (FunctionWrapper function : functions) {
			if (function.id >= legacyValues.storedFunctionValues.length) continue;
			checkReferenceMigration(
					legacyValues.hadReferenceFunctions[function.id], function.bitField,
					legacyValues.storedFunctionValues[function.id], function.classMethod::toString
			);
			function.bitField.fixLegacyTypes(read, legacyValues.storedFunctionValues[function.id]);
		}
	}

	void shallowCopy(Object original, Object target) {
		for (FieldWrapper fieldWrapper : fields) {
			fieldWrapper.bitField.field.setValue.accept(target, fieldWrapper.bitField.field.getValue.apply(original));
		}
	}

	boolean deepEquals(Object a, Object b, BitserCache cache) {
		for (FieldWrapper fieldWrapper : fields) {
			if (!fieldWrapper.bitField.deepEquals(
					fieldWrapper.bitField.field.getValue.apply(a),
					fieldWrapper.bitField.field.getValue.apply(b), cache
			)) return false;
		}
		return true;
	}

	int hashCode(Object value, BitserCache cache) {
		int code = 5;
		for (FieldWrapper fieldWrapper : fields) {
			code = 31 * code + fieldWrapper.bitField.hashCode(fieldWrapper.bitField.field.getValue.apply(value), cache);
		}
		return code;
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

	static class FunctionWrapper {

		final int id;
		final Method classMethod;
		final BitFieldWrapper bitField;

		FunctionWrapper(int id, Method classMethod, BitFieldWrapper bitField) {
			this.id = id;
			this.classMethod = classMethod;
			this.bitField = bitField;
		}

		Object computeValue(Object object, FunctionContext context) {
			try {
				if (classMethod.getParameterCount() == 0) return classMethod.invoke(object);
				else return classMethod.invoke(object, context);
			} catch (IllegalAccessException e) {
				throw new Error(e);
			} catch (InvocationTargetException e) {
				if (e.getCause() instanceof RuntimeException) throw (RuntimeException) e.getCause();
				if (e.getCause() instanceof Error) throw (Error) e.getCause();
				throw new RuntimeException(e.getCause());
			}
		}
	}
}
