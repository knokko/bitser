package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.backward.LegacyClasses;
import com.github.knokko.bitser.backward.instance.LegacyCollectionInstance;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.field.ClassField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.serialize.BitserCache;
import com.github.knokko.bitser.serialize.LabelCollection;
import com.github.knokko.bitser.serialize.ReadJob;
import com.github.knokko.bitser.serialize.WriteJob;
import com.github.knokko.bitser.util.ReferenceIdMapper;
import com.github.knokko.bitser.util.VirtualField;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

@BitStruct(backwardCompatible = false)
class BitCollectionFieldWrapper extends AbstractCollectionFieldWrapper {

	@ClassField(root = BitFieldWrapper.class)
	private final BitFieldWrapper valuesWrapper;

	BitCollectionFieldWrapper(VirtualField field, IntegerField sizeField, BitFieldWrapper valuesWrapper) {
		super(field, sizeField);
		this.valuesWrapper = valuesWrapper;
	}

	@SuppressWarnings("unused")
	private BitCollectionFieldWrapper() {
		super();
		this.valuesWrapper = null;
	}

	@Override
	ArrayType determineArrayType() {
		return null;
	}

	@Override
	void registerLegacyClasses(Object value, LegacyClasses legacy) {
		super.registerLegacyClasses(value, legacy);
		if (value == null) return;
		if (value.getClass().isArray()) {
			int size = Array.getLength(value);
			for (int index = 0; index < size; index++) valuesWrapper.registerLegacyClasses(Array.get(value, index), legacy);
		} else {
			for (Object element : (Collection<?>) value) valuesWrapper.registerLegacyClasses(element, legacy);
		}
	}

	@Override
	public BitFieldWrapper getChildWrapper() {
		return valuesWrapper;
	}

	@Override
	public void collectReferenceLabels(LabelCollection labels) {
		super.collectReferenceLabels(labels);
		valuesWrapper.collectReferenceLabels(labels);
	}

	@Override
	public void collectUsedReferenceLabels(LabelCollection labels, Object value) {
		super.collectReferenceLabels(labels);
		if (value == null) return;
		if (field.type.isArray()) {
			int size = Array.getLength(value);
			for (int index = 0; index < size; index++) {
				valuesWrapper.collectUsedReferenceLabels(labels, Array.get(value, index));
			}
		} else {
			for (Object element : (Collection<?>) value) {
				valuesWrapper.collectUsedReferenceLabels(labels, element);
			}
		}
	}

	@Override
	void registerReferenceTargets(Object value, BitserCache cache, ReferenceIdMapper idMapper) {
		super.registerReferenceTargets(value, cache, idMapper);
		if (value == null) return;
		if (field.type.isArray()) {
			int size = Array.getLength(value);
			for (int index = 0; index < size; index++) {
				valuesWrapper.registerReferenceTargets(Array.get(value, index), cache, idMapper);
			}
		} else {
			for (Object element : (Collection<?>) value) valuesWrapper.registerReferenceTargets(element, cache, idMapper);
		}
	}

	@Override
	void writeElements(Object value, int size, WriteJob write) throws IOException {
		String nullErrorMessage = "Field " + field + " must not have null elements";
		if (field.type.isArray()) {
			for (int index = 0; index < size; index++) {
				writeElement(Array.get(value, index), valuesWrapper, write, nullErrorMessage);
			}
		} else {
			for (Object element : (Collection<?>) value) {
				writeElement(element, valuesWrapper, write, nullErrorMessage);
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	void readElements(Object value, int size, ReadJob read) throws IOException {
		for (int index = 0; index < size; index++) {
			if (valuesWrapper.field.optional && !read.input.read()) {
				if (value instanceof Collection<?>) {
					((Collection<Object>) value).add(null);
				} else {
					Array.set(value, index, null);
				}
			} else {
				final int rememberIndex = index;
				List<Object> rememberElement = new ArrayList<>(1);
				valuesWrapper.readValue(read, element -> {
					rememberElement.add(element);
					if (value instanceof Collection<?>) {
						((Collection<Object>) value).add(element);
					} else {
						Array.set(value, rememberIndex, element);
					}
				});

				if (valuesWrapper.field.referenceTargetLabel != null) {
					read.idLoader.register(valuesWrapper.field.referenceTargetLabel, rememberElement.get(0), read.input, read.cache);
				}
			}
		}
	}

	@Override
	void setLegacyValue(ReadJob read, Object rawLegacyInstance, Consumer<Object> setValue) {
		if (rawLegacyInstance == null) {
			super.setLegacyValue(read, null, setValue);
			return;
		}

		LegacyCollectionInstance legacyInstance = (LegacyCollectionInstance) rawLegacyInstance;
		Object dummyArray = legacyInstance.newCollection.getClass().isArray() ? null : Array.newInstance(valuesWrapper.field.type, 1);
		int size = Array.getLength(legacyInstance.legacyArray);
		for (int index = 0; index < size; index++) {
			final int rememberIndex = index;
			final Object oldValue = Array.get(legacyInstance.legacyArray, index);

			try {
				if (legacyInstance.newCollection.getClass().isArray()) {
					valuesWrapper.setLegacyValue(read, oldValue, newValue ->
							Array.set(legacyInstance.newCollection, rememberIndex, newValue)
					);
				} else {
					valuesWrapper.setLegacyValue(read, oldValue, newValue -> {
						Array.set(dummyArray, 0, newValue);
						//noinspection unchecked
						((Collection<Object>) legacyInstance.newCollection).add(newValue);
					});
				}
			} catch (IllegalArgumentException wrongType) {
				throw new InvalidBitFieldException("Can't convert from legacy " + oldValue + " to " + valuesWrapper.field.type + " for field " + field);
			}
		}

		super.setLegacyValue(read, legacyInstance.newCollection, setValue);
	}

	@Override
	public void fixLegacyTypes(ReadJob read, Object value) {
		if (value != null && !(value instanceof LegacyCollectionInstance)) {
			throw new InvalidBitFieldException("Can't convert from legacy " + value + " to " + valuesWrapper.field.type + " for field " + field);
		}
		super.fixLegacyTypes(read, value);
		if (value == null) return;
		LegacyCollectionInstance legacyInstance = (LegacyCollectionInstance) value;
		int size = Array.getLength(legacyInstance.legacyArray);
		for (int index = 0; index < size; index++) {
			valuesWrapper.fixLegacyTypes(read, Array.get(legacyInstance.legacyArray, index));
		}
	}
}
