package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.UnexpectedBitserException;
import com.github.knokko.bitser.field.*;
import com.github.knokko.bitser.util.JobOutput;
import com.github.knokko.bitser.util.Recursor;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

import static com.github.knokko.bitser.WrapperFactory.createComplexWrapper;

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
				ClassField.class, EnumField.class, FloatField.class, IntegerField.class, NestedFieldSetting.class,
				NestedFieldSettings.class, ReferenceField.class, ReferenceFieldTarget.class,
				StableReferenceFieldId.class, StringField.class
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
				if (classField.getType() == SimpleLazyBits.class) bitField = DEFAULT_BIT_FIELD;
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
				if (Modifier.isFinal(classField.getModifiers()) || !Modifier.isPublic(classField.getModifiers()) ||
						!Modifier.isPublic(classField.getDeclaringClass().getModifiers())) {
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
								throw new UnexpectedBitserException("Failed to access " + classField);
							}
						},
						(target, newValue) -> {
							try {
								classField.set(target, newValue);
							} catch (IllegalAccessException e) {
								throw new UnexpectedBitserException("Failed to access " + classField);
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

	List<FieldWrapper> getFields(boolean backwardCompatible) {
		return backwardCompatible ? fieldsSortedById : fields;
	}

	@Override
	public String toString() {
		return myClass.getName();
	}

	JobOutput<LegacyClass> register(Object object, Recursor<LegacyClasses, LegacyInfo> recursor) {
		JobOutput<LegacyClass> legacyClass = recursor.computeFlat(myClass.getSimpleName(), legacy ->
				legacy.addClass(myClass)
		);

		for (int index = 0; index < fieldsSortedById.size(); index++) {
			FieldWrapper field = fieldsSortedById.get(index);
			final int rememberIndex = index;
			recursor.runFlat(field.classField.getName(), legacy -> {
				if (legacyClass.get().fields.size() == rememberIndex) {
					legacyClass.get().fields.add(new LegacyField(field.id, field.bitField));
				}
			});
			recursor.runNested(field.classField.getName(), nested ->
					field.bitField.registerLegacyClasses(field.bitField.field.getValue.apply(object), nested)
			);
		}

		for (int index = 0; index < functions.size(); index++) {
			FunctionWrapper function = functions.get(index);
			final int rememberIndex = index;
			recursor.runFlat(function.classMethod.getName(), legacy -> {
				if (legacyClass.get().functions.size() == rememberIndex) {
					legacyClass.get().functions.add(new LegacyField(function.id, function.bitField));
				}
			});

			recursor.runNested(function.classMethod.getName(), nested -> {
				Object returnValue = function.computeValue(object, recursor.info.functionContext);
				function.bitField.registerLegacyClasses(returnValue, nested);
			});
		}

		return legacyClass;
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

		Object computeValue(Object object, FunctionContext context) throws Throwable {
			try {
				if (classMethod.getParameterCount() == 0) return classMethod.invoke(object);
				else return classMethod.invoke(object, context);
			} catch (InvocationTargetException e) {
				throw e.getCause();
			}
		}
	}
}
