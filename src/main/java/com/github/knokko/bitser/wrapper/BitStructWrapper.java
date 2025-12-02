package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.backward.*;
import com.github.knokko.bitser.backward.instance.LegacyStructInstance;
import com.github.knokko.bitser.backward.instance.LegacyValues;
import com.github.knokko.bitser.connection.BitStructConnection;
import com.github.knokko.bitser.context.*;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.LegacyBitserException;
import com.github.knokko.bitser.exceptions.UnexpectedBitserException;
import com.github.knokko.bitser.serialize.BitPostInit;
import com.github.knokko.bitser.serialize.*;
import com.github.knokko.bitser.util.JobOutput;
import com.github.knokko.bitser.util.Recursor;
import com.github.knokko.bitser.util.ReferenceIdMapper;
import com.github.knokko.bitser.util.VirtualField;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Consumer;

public class BitStructWrapper<T> {

	public static <T> BitStructWrapper<T> wrap(Class<T> objectClass) {
		BitStruct bitStruct = objectClass.getAnnotation(BitStruct.class);
		if (bitStruct != null) return new BitStructWrapper<>(objectClass, bitStruct);

		throw new InvalidBitFieldException(objectClass + " is not a BitStruct");
	}

	private final BitStruct bitStruct;
	private final List<SingleClassWrapper> classHierarchy;
	private final Constructor<T> constructor;
	private final VirtualField stableIdField;

	BitStructWrapper(Class<T> objectClass, BitStruct bitStruct) {
		if (bitStruct == null) {
			throw new InvalidBitFieldException("Class must have a BitStruct annotation: " + objectClass);
		}
		this.bitStruct = bitStruct;

		if (Modifier.isAbstract(objectClass.getModifiers())) {
			throw new InvalidBitFieldException(objectClass + " is abstract");
		}
		if (Modifier.isInterface(objectClass.getModifiers())) {
			throw new InvalidBitFieldException(objectClass + " is an interface");
		}

		try {
			this.constructor = objectClass.getDeclaredConstructor();
			try {
				constructor.newInstance();
			} catch (IllegalAccessException e) {
				constructor.setAccessible(true);
			} catch (InstantiationException shouldNotHappen) {
				throw new InvalidBitFieldException(
						"Class " + objectClass + " cannot be instantiated: " + shouldNotHappen.getMessage()
				);
			} catch (InvocationTargetException failedConstruction) {
				throw new InvalidBitFieldException(
						"The constructor of " + objectClass + " failed: " + failedConstruction.getMessage()
				);
			}
		} catch (NoSuchMethodException e) {
			throw new InvalidBitFieldException(objectClass + " must have a constructor without parameters");
		}

		this.classHierarchy = new ArrayList<>();
		Class<?> currentClass = objectClass;
		while (currentClass != null) {
			this.classHierarchy.add(new SingleClassWrapper(currentClass, bitStruct.backwardCompatible()));
			currentClass = currentClass.getSuperclass();
		}

		this.stableIdField = findStableField(objectClass);
	}

	private VirtualField findStableField(Class<T> objectClass) {
		VirtualField stableIdField = null;

		for (SingleClassWrapper currentClass : classHierarchy) {
			for (SingleClassWrapper.FieldWrapper field : currentClass.fields) {
				if (field.bitField instanceof UUIDFieldWrapper && ((UUIDFieldWrapper) field.bitField).isStableReferenceId) {
					if (stableIdField != null) throw new InvalidBitFieldException(
							"Bit struct " + objectClass + " has multiple stable ID fields, but at most 1 is allowed"
					);
					stableIdField = field.bitField.field;
				}
			}
		}

		return stableIdField;
	}

	public void collectReferenceLabels(LabelCollection labels) {
		if (labels.visitedStructs.contains(this)) return;
		labels.visitedStructs.add(this);
		for (SingleClassWrapper currentClass : classHierarchy) {
			currentClass.collectReferenceLabels(labels);
		}
	}

	public void registerReferenceTargets(Object object, Recursor<ReferenceIdMapper, BitserCache> recursor) {
		for (SingleClassWrapper currentClass : classHierarchy) {
			recursor.runNested(currentClass.myClass.getSimpleName(), child ->
					currentClass.registerReferenceTargets(object, child)
			);
		}
	}

	public JobOutput<LegacyStruct> registerClasses(Object object, Recursor<LegacyClasses, LegacyInfo> recursor) {
		if (!this.bitStruct.backwardCompatible()) {
			throw new InvalidBitFieldException("BitStruct " + classHierarchy.get(0) + " is not backward compatible");
		}

		JobOutput<LegacyStruct> legacyStruct = recursor.computeFlat("declaring class", legacy ->
				legacy.addStruct(constructor.getDeclaringClass())
		);
		for (int index = 0; index < classHierarchy.size(); index++) {
			final int rememberIndex = index;
			SingleClassWrapper currentClass = classHierarchy.get(index);
			JobOutput<LegacyClass> registered = currentClass.register(object, recursor);
			recursor.runFlat(currentClass.myClass.getSimpleName(), legacy -> {
				if (legacyStruct.get().classHierarchy.size() == rememberIndex) {
					legacyStruct.get().classHierarchy.add(registered.get());
				}
			});
		}
		return legacyStruct;
	}

	public UUID getStableId(Object target) {
		if (stableIdField == null) throw new InvalidBitFieldException(target + " doesn't have an @StableReferenceFieldId");
		return (UUID) stableIdField.getValue.apply(target);
	}

	public void write(Object object, Recursor<WriteContext, WriteInfo> recursor) {
		if (recursor.info.legacy != null && !bitStruct.backwardCompatible()) {
			throw new InvalidBitFieldException("BitStruct " + classHierarchy.get(0) + " is not backward compatible");
		}
		for (SingleClassWrapper currentClass : classHierarchy) {
			currentClass.write(object, recursor);
		}
	}

	private T createEmptyInstance() {
		try {
			return constructor.newInstance();
		} catch (InstantiationException e) {
			throw new UnexpectedBitserException("Failed to instantiate " + constructor);
		} catch (IllegalAccessException shouldNotHappen) {
			throw new UnexpectedBitserException("Can't get access to " + constructor);
		} catch (InvocationTargetException e) {
			throw new InvalidBitFieldException("Constructor " + constructor + " throw an exception: " + e.getMessage());
		}
	}

	public void read(Recursor<ReadContext, ReadInfo> recursor, ValueConsumer setValue) throws IOException {
		T object = createEmptyInstance();
		Map<Class<?>, Object[]> serializedFunctionValues = new HashMap<>();
		for (SingleClassWrapper currentClass : classHierarchy) {
			serializedFunctionValues.put(currentClass.myClass, currentClass.read(object, recursor));
		}
		if (object instanceof BitPostInit) {
			recursor.runFlat("post-resolve", context ->
				context.idLoader.addPostResolveCallback(() -> ((BitPostInit) object).postInit(
						new BitPostInit.Context(
								recursor.info.bitser, serializedFunctionValues, null,
								null, recursor.info.withParameters
						)
				))
			);
		}
		setValue.consume(object);
	}

	public T setLegacyValues(Recursor<ReadContext, ReadInfo> recursor, LegacyStructInstance legacy) {
		for (int index = 0; index < classHierarchy.size(); index++) {
			classHierarchy.get(index).setLegacyValues(recursor, legacy.newInstance, legacy.valuesHierarchy.get(index));
		}

		if (legacy.newInstance instanceof BitPostInit) {
			recursor.runFlat("post-resolve", context ->
					context.idLoader.addPostResolveCallback(() -> {
						Map<Class<?>, Object[]> functionValues = new HashMap<>();
						Map<Class<?>, Object[]> legacyFieldValues = new HashMap<>();
						Map<Class<?>, Object[]> legacyFunctionValues = new HashMap<>();
						for (int index = 0; index < classHierarchy.size(); index++) {
							LegacyValues classLegacy = legacy.valuesHierarchy.get(index);
							functionValues.put(classHierarchy.get(index).myClass, classLegacy.convertedFunctionValues);
							legacyFieldValues.put(classHierarchy.get(index).myClass, classLegacy.values);
							legacyFunctionValues.put(classHierarchy.get(index).myClass, classLegacy.storedFunctionValues);
						}
						((BitPostInit) legacy.newInstance).postInit(new BitPostInit.Context(
								recursor.info.bitser, functionValues, legacyFieldValues,
								legacyFunctionValues, recursor.info.withParameters
						));
					})
			);
		}

		//noinspection unchecked
		return (T) legacy.newInstance;
	}

	public void fixLegacyTypes(Recursor<ReadContext, ReadInfo> recursor, LegacyStructInstance legacyInstance) {
		if (legacyInstance.valuesHierarchy.size() != classHierarchy.size()) {
			throw new LegacyBitserException(
					"Class hierarchy size changed from " + legacyInstance.valuesHierarchy.size() +
							" to " + classHierarchy.size()
			);
		}
		legacyInstance.newInstance = createEmptyInstance();
		for (int index = 0; index < classHierarchy.size(); index++) {
			classHierarchy.get(index).fixLegacyTypes(recursor, legacyInstance.valuesHierarchy.get(index));
		}
	}

	public T shallowCopy(Object original) {
		T copy = createEmptyInstance();
		for (SingleClassWrapper currentClass : classHierarchy) {
			currentClass.shallowCopy(original, copy);
		}
		return copy;
	}

	public boolean deepEquals(Object a, Object b, BitserCache cache) {
		for (SingleClassWrapper currentClass : classHierarchy) {
			if (!currentClass.deepEquals(a, b, cache)) return false;
		}
		return true;
	}

	public int hashCode(Object value, BitserCache cache) {
		if (value == null) return 1;
		int code = 2;
		for (SingleClassWrapper currentClass : classHierarchy) {
			code = 13 * code + currentClass.hashCode(value, cache);
		}
		return code;
	}

	public <C> BitStructConnection<C> createConnection(
			Bitser bitser, C object, Consumer<BitStructConnection.ChangeListener> reportChanges
	) {
		List<BitFieldWrapper> fields = new ArrayList<>();
		Map<String, BitFieldWrapper> nameToChildMapping = new HashMap<>();
		for (SingleClassWrapper currentClass : classHierarchy) {
			for (SingleClassWrapper.FieldWrapper field : currentClass.fields) {
				fields.add(field.bitField);
				if (field.bitField instanceof StructFieldWrapper || List.class.isAssignableFrom(field.bitField.field.type)) {
					if (nameToChildMapping.containsKey(field.classField.getName())) {
						throw new InvalidBitFieldException("Class " + classHierarchy.get(0) + " has multiple nested fields named " + field.classField.getName());
					}
					nameToChildMapping.put(field.classField.getName(), field.bitField);
				}
			}
		}
		return new BitStructConnection<>(bitser, fields, nameToChildMapping, object, reportChanges);
	}
}
