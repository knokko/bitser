package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.backward.*;
import com.github.knokko.bitser.backward.instance.LegacyStructInstance;
import com.github.knokko.bitser.backward.instance.LegacyValues;
import com.github.knokko.bitser.connection.BitStructConnection;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.serialize.BitPostInit;
import com.github.knokko.bitser.serialize.*;
import com.github.knokko.bitser.util.VirtualField;
import com.github.knokko.bitser.util.ReferenceIdMapper;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Consumer;

class BitStructWrapper<T> extends BitserWrapper<T> {

	private final BitStruct bitStruct;
	private final List<SingleClassWrapper> classHierarchy;
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

	@Override
	public void collectReferenceLabels(LabelCollection labels) {
		if (labels.visitedStructs.contains(this)) return;
		labels.visitedStructs.add(this);
		for (SingleClassWrapper currentClass : classHierarchy) {
			currentClass.collectReferenceLabels(labels);
		}
	}

	@Override
	public void collectUsedReferenceLabels(LabelCollection labels, Object value) {
		if (labels.visitedStructs.contains(this)) return;
		labels.visitedStructs.add(this);
		for (SingleClassWrapper currentClass : classHierarchy) {
			currentClass.collectUsedReferenceLabels(labels, value);
		}
	}

	@Override
	public void registerReferenceTargets(Object object, BitserCache cache, ReferenceIdMapper mapper) {
		for (SingleClassWrapper currentClass : classHierarchy) {
			currentClass.registerReferenceTargets(object, cache, mapper);
		}
	}

	@Override
	public LegacyStruct registerClasses(Object object, LegacyClasses legacy) {
		if (!this.bitStruct.backwardCompatible()) {
			throw new InvalidBitFieldException("BitStruct " + classHierarchy.get(0) + " is not backward compatible");
		}
		LegacyStruct legacyStruct = legacy.addStruct(constructor.getDeclaringClass());
		for (int index = 0; index < classHierarchy.size(); index++) {
			SingleClassWrapper currentClass = classHierarchy.get(index);
			LegacyClass registered = currentClass.register(object, legacy);
			if (legacyStruct.classHierarchy.size() == index) {
				legacyStruct.classHierarchy.add(registered);
			}
		}
		return legacyStruct;
	}

	@Override
	public UUID getStableId(Object target) {
		if (stableIdField == null) throw new InvalidBitFieldException(target + " doesn't have an @StableReferenceFieldId");
		return (UUID) stableIdField.getValue.apply(target);
	}

	@Override
	public void write(Object object, WriteJob write) throws IOException {
		if (write.legacy != null && !bitStruct.backwardCompatible()) {
			throw new InvalidBitFieldException("BitStruct " + classHierarchy.get(0) + " is not backward compatible");
		}
		for (SingleClassWrapper currentClass : classHierarchy) {
			currentClass.write(object, write);
		}
	}

	private T createEmptyInstance() {
		try {
			return constructor.newInstance();
		} catch (InstantiationException e) {
			throw new Error("Failed to instantiate " + constructor, e);
		} catch (IllegalAccessException shouldNotHappen) {
			throw new Error(shouldNotHappen);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void read(ReadJob read, ValueConsumer setValue) throws IOException {
		T object = createEmptyInstance();
		Map<Class<?>, Object[]> serializedFunctionValues = new HashMap<>();
		for (SingleClassWrapper currentClass : classHierarchy) {
			serializedFunctionValues.put(currentClass.myClass, currentClass.read(object, read));
		}
		if (object instanceof BitPostInit) {
			read.idLoader.addPostResolveCallback(() -> ((BitPostInit) object).postInit(
					new BitPostInit.Context(
							read.bitser, serializedFunctionValues, null,
							null, read.withParameters
					)
			));
		}
		setValue.consume(object);
	}

	@Override
	public T setLegacyValues(ReadJob read, LegacyStructInstance legacy) {
		if (legacy.valuesHierarchy.size() != classHierarchy.size()) {
			throw new InvalidBitFieldException("Inconsistent class hierarchy");
		}
		for (int index = 0; index < classHierarchy.size(); index++) {
			classHierarchy.get(index).setLegacyValues(read, legacy.newInstance, legacy.valuesHierarchy.get(index));
		}

		if (legacy.newInstance instanceof BitPostInit) {
			read.idLoader.addPostResolveCallback(() -> {
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
						read.bitser, functionValues, legacyFieldValues, legacyFunctionValues, read.withParameters
				));
			});
		}

		//noinspection unchecked
		return (T) legacy.newInstance;
	}

	@Override
	public void fixLegacyTypes(ReadJob read, LegacyStructInstance legacyInstance) {
		legacyInstance.newInstance = createEmptyInstance();
		for (int index = 0; index < classHierarchy.size(); index++) {
			classHierarchy.get(index).fixLegacyTypes(read, legacyInstance.valuesHierarchy.get(index));
		}
	}

	@Override
	public T shallowCopy(Object original) {
		T copy = createEmptyInstance();
		for (SingleClassWrapper currentClass : classHierarchy) {
			currentClass.shallowCopy(original, copy);
		}
		return copy;
	}

	@Override
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
						throw new Error("Class " + classHierarchy.get(0) + " has multiple nested fields named " + field.classField.getName());
					}
					nameToChildMapping.put(field.classField.getName(), field.bitField);
				}
			}
		}
		return new BitStructConnection<>(bitser, fields, nameToChildMapping, object, reportChanges);
	}
}
