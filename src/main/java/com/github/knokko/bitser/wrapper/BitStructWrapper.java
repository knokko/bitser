package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.connection.BitStructConnection;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.Bitser;
import com.github.knokko.bitser.serialize.BitserCache;
import com.github.knokko.bitser.util.VirtualField;
import com.github.knokko.bitser.util.ReferenceIdLoader;
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
	public void collectReferenceTargetLabels(
			BitserCache cache, Set<String> declaredTargetLabels,
			Set<String> stableLabels, Set<String> unstableLabels, Set<Object> visitedStructs
	) {
		if (visitedStructs.contains(this)) return;
		visitedStructs.add(this);
		for (SingleClassWrapper currentClass : classHierarchy) {
			currentClass.collectReferenceTargetLabels(cache, declaredTargetLabels, stableLabels, unstableLabels, visitedStructs);
		}
	}

	@Override
	public void registerReferenceTargets(Object object, BitserCache cache, ReferenceIdMapper mapper) {
		for (SingleClassWrapper currentClass : classHierarchy) {
			currentClass.registerReferenceTargets(object, cache, mapper);
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
		for (SingleClassWrapper currentClass : classHierarchy) {
			// TODO Backward compatible?
			currentClass.write(object, output, cache, idMapper, false);
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
	public void read(
			BitInputStream input, BitserCache cache, ReferenceIdLoader idLoader, ValueConsumer setValue
	) throws IOException {
		if (bitStruct.backwardCompatible()) throw new UnsupportedOperationException("TODO");
		T object = createEmptyInstance();
		for (SingleClassWrapper currentClass : classHierarchy) {
			currentClass.read(object, input, cache, idLoader, false);
		}
		setValue.consume(object);
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
