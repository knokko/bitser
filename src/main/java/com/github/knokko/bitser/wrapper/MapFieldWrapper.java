package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.backward.LegacyClasses;
import com.github.knokko.bitser.backward.instance.LegacyMapInstance;
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
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.BiConsumer;
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
	public void collectReferenceLabels(LabelCollection labels) {
		super.collectReferenceLabels(labels);
		keysWrapper.collectReferenceLabels(labels);
		valuesWrapper.collectReferenceLabels(labels);
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
		LegacyMapInstance legacyInstance = read.backwardCompatible ? new LegacyMapInstance((HashMap<?, ?>) map) : null;
		for (int counter = 0; counter < size; counter++) {
			DelayedEntry delayed = new DelayedEntry((key, value) -> {
				//noinspection unchecked
				((Map<Object, Object>) map).put(key, value);
			});
			readElement(keysWrapper, read, key -> {
				if (legacyInstance != null) legacyInstance.legacyKeys.add(key);
				delayed.setKey(key);
			});
			readElement(valuesWrapper, read, value -> {
				if (legacyInstance != null) legacyInstance.legacyValues.add(value);
				delayed.setValue(value);
			});
		}

		if (read.backwardCompatible) setValue.consume(legacyInstance);
		else setValue.consume(map);
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
				read.idLoader.register(
						wrapper.field.referenceTargetLabel,
						rememberElement.get(0), read.input, read.bitser.cache
				);
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
	void setLegacyValue(ReadJob read, Object rawLegacyMap, Consumer<Object> setValue) {
		if (rawLegacyMap == null) {
			super.setLegacyValue(read, null, setValue);
			return;
		}

		LegacyMapInstance legacyInstance = (LegacyMapInstance) rawLegacyMap;

		Object dummyKeyArray = Array.newInstance(keysWrapper.field.type, 1);
		Object dummyValueArray = Array.newInstance(valuesWrapper.field.type, 1);

		legacyInstance.legacyMap.forEach((legacyKey, legacyValue) -> {
			DelayedEntry delayed = new DelayedEntry((newKey, newValue) -> {
				try {
					Array.set(dummyKeyArray, 0, newKey);
				} catch (IllegalArgumentException wrongType) {
					throw new InvalidBitFieldException("Can't convert from legacy " + legacyKey + " to " + keysWrapper.field.type + " for field " + field);
				}
				try {
					Array.set(dummyValueArray, 0, newValue);
				} catch (IllegalArgumentException wrongType) {
					throw new InvalidBitFieldException("Can't convert from legacy " + legacyValue + " to " + valuesWrapper.field.type + " for field " + field);
				}

				//noinspection unchecked
				((Map<Object, Object>)legacyInstance.newMap).put(newKey, newValue);
			});
			keysWrapper.setLegacyValue(read, legacyKey, delayed::setKey);
			valuesWrapper.setLegacyValue(read, legacyValue, delayed::setValue);
		});

		super.setLegacyValue(read, legacyInstance.newMap, setValue);
	}

	@Override
	public void fixLegacyTypes(ReadJob read, Object rawLegacyInstance) {
		if (rawLegacyInstance == null && field.optional) return;
		assert rawLegacyInstance != null;
		LegacyMapInstance legacyInstance = (LegacyMapInstance) rawLegacyInstance;
		legacyInstance.newMap = (Map<?, ?>) constructCollectionWithSize(
				field.type, legacyInstance.legacyMap.size()
		);
		if (field.referenceTargetLabel != null) {
			read.idLoader.replace(field.referenceTargetLabel, legacyInstance, legacyInstance.newMap);
		}

		for (Object key : legacyInstance.legacyKeys) keysWrapper.fixLegacyTypes(read, key);
		for (Object value : legacyInstance.legacyValues) valuesWrapper.fixLegacyTypes(read, value);
	}

	private static class DelayedEntry {

		boolean hasKey = false;
		boolean hasValue = false;
		Object key = null;
		Object value = null;

		final BiConsumer<Object, Object> insert;

		DelayedEntry(BiConsumer<Object, Object> insert) {
			this.insert = insert;
		}

		private void maybeInsert() {
			if (hasKey && hasValue) {
				insert.accept(key, value);
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

	@Override
	boolean deepEquals(Object a, Object b, BitserCache cache) {
		if (a == null && b == null) return true;
		if (a == null || b == null) return false;
		Iterator<? extends Map.Entry<?, ?>> iteratorA = ((Map<?, ?>) a).entrySet().iterator();
		Iterator<? extends Map.Entry<?, ?>> iteratorB = ((Map<?, ?>) b).entrySet().iterator();
		while (true) {
			if (iteratorA.hasNext() != iteratorB.hasNext()) return false;
			if (!iteratorA.hasNext()) return true;
			Map.Entry<?, ?> entryA = iteratorA.next();
			Map.Entry<?, ?> entryB = iteratorB.next();
			if (!keysWrapper.deepEquals(entryA.getKey(), entryB.getKey(), cache)) return false;
			if (!valuesWrapper.deepEquals(entryA.getValue(), entryB.getValue(), cache)) return false;
		}
	}

	@Override
	int hashCode(Object value, BitserCache cache) {
		if (value == null) return -12;

		int code = 15;
		for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
			code = 3 * code + keysWrapper.hashCode(entry.getKey(), cache);
			code = 5 * code + valuesWrapper.hashCode(entry.getValue(), cache);
		}

		return code;
	}
}
