package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.backward.LegacyClasses;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.ClassField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.serialize.BitserCache;
import com.github.knokko.bitser.serialize.LabelCollection;
import com.github.knokko.bitser.serialize.ReadJob;
import com.github.knokko.bitser.serialize.WriteJob;
import com.github.knokko.bitser.util.ReferenceIdMapper;
import com.github.knokko.bitser.util.VirtualField;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Consumer;

import static com.github.knokko.bitser.serialize.IntegerBitser.*;
import static com.github.knokko.bitser.serialize.IntegerBitser.decodeVariableInteger;
import static com.github.knokko.bitser.wrapper.AbstractCollectionFieldWrapper.constructCollectionWithSize;
import static com.github.knokko.bitser.wrapper.AbstractCollectionFieldWrapper.writeElement;
import static java.lang.Math.max;
import static java.lang.Math.min;

@BitStruct(backwardCompatible = false)
class MapFieldWrapper extends BitFieldWrapper {

	@BitField
	private final IntegerField.Properties sizeField;

	@ClassField(root = BitFieldWrapper.class)
	private final BitFieldWrapper keysWrapper;

	@ClassField(root = BitFieldWrapper.class)
	private final BitFieldWrapper valuesWrapper;

	MapFieldWrapper(VirtualField field, IntegerField sizeField, BitFieldWrapper keysWrapper, BitFieldWrapper valuesWrapper) {
		super(field);
		if (sizeField.minValue() > Integer.MAX_VALUE || sizeField.maxValue() < 0) throw new IllegalArgumentException();
		if (field.type.isInterface() || Modifier.isAbstract(field.type.getModifiers())) {
			throw new InvalidBitFieldException("Field type must not be abstract or an interface: " + field);
		}
		this.sizeField = new IntegerField.Properties(sizeField);
		this.keysWrapper = keysWrapper;
		this.valuesWrapper = valuesWrapper;
	}

	@SuppressWarnings("unused")
	private MapFieldWrapper() {
		super();
		this.sizeField = new IntegerField.Properties();
		this.keysWrapper = null;
		this.valuesWrapper = null;
	}

	@Override
	void registerLegacyClasses(Object map, LegacyClasses legacy) {
		super.registerLegacyClasses(map, legacy);
		if (map == null) return;
		((Map<?, ?>) map).forEach((key, value) -> {
			keysWrapper.registerLegacyClasses(key, legacy);
			valuesWrapper.registerLegacyClasses(value, legacy);
		});
	}

	@Override
	public void collectReferenceTargetLabels(LabelCollection labels) {
		super.collectReferenceTargetLabels(labels);
		keysWrapper.collectReferenceTargetLabels(labels);
		valuesWrapper.collectReferenceTargetLabels(labels);
	}

	@Override
	void registerReferenceTargets(Object rawValue, BitserCache cache, ReferenceIdMapper idMapper) {
		super.registerReferenceTargets(rawValue, cache, idMapper);
		if (rawValue == null) return;
		Map<?, ?> map = (Map<?, ?>) rawValue;
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			keysWrapper.registerReferenceTargets(entry.getKey(), cache, idMapper);
			valuesWrapper.registerReferenceTargets(entry.getValue(), cache, idMapper);
		}
	}

	@Override
	void writeValue(Object rawValue, WriteJob write) throws IOException {
		Map<?, ?> map = (Map<?, ?>) rawValue;
		if (sizeField.expectUniform) encodeUniformInteger(map.size(), getMinSize(), getMaxSize(), write.output);
		else encodeVariableInteger(map.size(), getMinSize(), getMaxSize(), write.output);

		for (Map.Entry<?, ?> entry : map.entrySet()) {
			writeElement(entry.getKey(), keysWrapper, write, "Field " + field + " must not have null keys");
			writeElement(entry.getValue(), valuesWrapper, write, "Field " + field + " must not have null values");
		}
	}

	@Override
	void readValue(ReadJob read, ValueConsumer setValue) throws IOException {
		int size;
		if (sizeField.expectUniform) size = (int) decodeUniformInteger(getMinSize(), getMaxSize(), read.input);
		else size = (int) decodeVariableInteger(getMinSize(), getMaxSize(), read.input);

		Map<?, ?> map = (Map<?, ?>) constructCollectionWithSize(field.type != null ? field.type : HashMap.class, size);
		for (int counter = 0; counter < size; counter++) {
			DelayedEntry delayed = new DelayedEntry(map);
			readElement(keysWrapper, read, delayed::setKey);
			readElement(valuesWrapper, read, delayed::setValue);
		}
		setValue.consume(map);
	}

	private void readElement(BitFieldWrapper wrapper, ReadJob read, ValueConsumer setValue) throws IOException {
		if (wrapper.field.optional && !read.input.read()) {
			setValue.consume(null);
		} else {
			List<Object> rememberElement = new ArrayList<>(1);
			wrapper.readValue(read, element -> {
				rememberElement.add(element);
				setValue.consume(element);
			});

			if (wrapper.field.referenceTargetLabel != null) {
				read.idLoader.register(wrapper.field.referenceTargetLabel, rememberElement.get(0), read.input, read.cache);
			}
		}
	}

	private int getMinSize() {
		return (int) max(0, sizeField.minValue);
	}

	private int getMaxSize() {
		return (int) min(Integer.MAX_VALUE, sizeField.maxValue);
	}

	@Override
	void setLegacyValue(ReadJob read, Object legacyMap, Consumer<Object> setValue) {
		if (!keysWrapper.delayLegacyUntilResolve() && !valuesWrapper.delayLegacyUntilResolve()) {
			actuallySetLegacyValues(read, legacyMap, setValue);
		}
	}

	@Override
	void setLegacyReference(ReadJob read, Object legacyMap, Consumer<Object> setValue) {
		if (keysWrapper.delayLegacyUntilResolve() || valuesWrapper.delayLegacyUntilResolve()) {
			actuallySetLegacyValues(read, legacyMap, setValue);
		}
	}

	private void actuallySetLegacyValues(ReadJob read, Object rawLegacyMap, Consumer<Object> setValue) {
		if (rawLegacyMap == null) {
			super.setLegacyValue(read, null, setValue);
			return;
		}

		Map<?, ?> legacyMap = (Map<?, ?>) rawLegacyMap;

		@SuppressWarnings("unchecked")
		Map<Object, Object> newMap = (Map<Object, Object>) constructCollectionWithSize(field.type, legacyMap.size());
		legacyMap.forEach((oldKey, oldValue) -> {
			int[] pCount = { 0 };
			Object[] pKey = { null };
			Object[] pValue = { null };

			if (keysWrapper.delayLegacyUntilResolve()) {
				keysWrapper.setLegacyReference(read, oldKey, newKey-> {
					pKey[0] = newKey;
					pCount[0] += 1;
					if (pCount[0] == 2) newMap.put(pKey[0], pValue[0]);
				});
			} else {
				keysWrapper.setLegacyValue(read, oldKey, newKey -> {
					pKey[0] = newKey;
					pCount[0] += 1;
					if (pCount[0] == 2) newMap.put(pKey[0], pValue[0]);
				});
			}

			if (valuesWrapper.delayLegacyUntilResolve()) {
				valuesWrapper.setLegacyReference(read, oldValue, newValue -> {
					pValue[0] = newValue;
					pCount[0] += 1;
					if (pCount[0] == 2) newMap.put(pKey[0], pValue[0]);
				});
			} else {
				valuesWrapper.setLegacyValue(read, oldValue, newValue -> {
					pValue[0] = newValue;
					pCount[0] += 1;
					if (pCount[0] == 2) newMap.put(pKey[0], pValue[0]);
				});
			}
		});

		super.setLegacyValue(read, newMap, setValue);
	}

	@Override
	boolean delayLegacyUntilResolve() {
		return keysWrapper.delayLegacyUntilResolve() || valuesWrapper.delayLegacyUntilResolve();
	}

	private static class DelayedEntry {

		boolean hasKey = false;
		boolean hasValue = false;
		Object key = null;
		Object value = null;

		final Map<?, ?> destination;

		DelayedEntry(Map<?, ?> destination) {
			this.destination = destination;
		}

		private void maybeInsert() {
			if (hasKey && hasValue) {
				//noinspection unchecked
				((Map<Object, Object>) destination).put(key, value);
			}
		}

		void setKey(Object key) {
			if (hasKey) throw new IllegalStateException();
			hasKey = true;
			this.key = key;
			maybeInsert();
		}

		void setValue(Object value) {
			if (hasValue) throw new IllegalStateException();
			hasValue = true;
			this.value = value;
			maybeInsert();
		}
	}
}
