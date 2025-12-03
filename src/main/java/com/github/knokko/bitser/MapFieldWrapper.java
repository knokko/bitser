package com.github.knokko.bitser;

import com.github.knokko.bitser.legacy.LegacyMapInstance;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.UnexpectedBitserException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.ClassField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.util.JobOutput;
import com.github.knokko.bitser.util.Recursor;

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.github.knokko.bitser.IntegerBitser.*;
import static com.github.knokko.bitser.IntegerBitser.decodeVariableInteger;
import static com.github.knokko.bitser.AbstractCollectionFieldWrapper.constructCollectionWithSize;
import static com.github.knokko.bitser.AbstractCollectionFieldWrapper.writeElement;
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
		if (sizeField.minValue() > Integer.MAX_VALUE || sizeField.maxValue() < 0) {
			throw new InvalidBitFieldException("Invalid sizeField bounds");
		}
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
	void registerLegacyClasses(Object map, Recursor<LegacyClasses, LegacyInfo> recursor) {
		super.registerLegacyClasses(map, recursor);
		if (map == null) return;
		((Map<?, ?>) map).forEach((key, value) -> {
			recursor.runNested("key", nested ->
					keysWrapper.registerLegacyClasses(key, nested)
			);
			recursor.runNested("value", nested ->
					valuesWrapper.registerLegacyClasses(value, nested)
			);
		});
	}

	@Override
	void collectReferenceLabels(LabelCollection labels) {
		super.collectReferenceLabels(labels);
		keysWrapper.collectReferenceLabels(labels);
		valuesWrapper.collectReferenceLabels(labels);
	}

	@Override
	void registerReferenceTargets(Object rawValue, Recursor<ReferenceIdMapper, BitserCache> recursor) {
		super.registerReferenceTargets(rawValue, recursor);
		if (rawValue == null) return;
		Map<?, ?> map = (Map<?, ?>) rawValue;
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			recursor.runNested("key " + entry.getKey(), nested ->
					keysWrapper.registerReferenceTargets(entry.getKey(), nested)
			);
			recursor.runNested("value for " + entry.getKey(), nested ->
					valuesWrapper.registerReferenceTargets(entry.getValue(), nested)
			);
		}
	}

	@Override
	void writeValue(Object rawValue, Recursor<WriteContext, WriteInfo> recursor) {
		Map<?, ?> map = (Map<?, ?>) rawValue;
		recursor.runFlat("size", context -> {
			context.output.prepareProperty("map-size", -1);
			if (sizeField.expectUniform) encodeUniformInteger(map.size(), getMinSize(), getMaxSize(), context.output);
			else encodeVariableInteger(map.size(), getMinSize(), getMaxSize(), context.output);
			context.output.finishProperty();
		});

		int counter = 0;
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			final int rememberCounter = counter;
			recursor.runFlat("pushContext", context ->
					context.output.pushContext("key", rememberCounter)
			);
			recursor.runNested("key " + entry.getKey(), nested -> writeElement(
					entry.getKey(), keysWrapper, nested,
					"Field " + field + " must not have null keys"
			));
			recursor.runFlat("context", context -> {
				context.output.popContext("key", rememberCounter);
				context.output.pushContext("value", rememberCounter);
			});
			recursor.runNested("value for " + entry.getKey(), nested -> writeElement(
					entry.getValue(), valuesWrapper, nested,
					"Field " + field + " must not have null values"
			));
			recursor.runFlat("popContext", context ->
					context.output.popContext("value", rememberCounter)
			);
			counter += 1;
		}
	}

	@Override
	void readValue(Recursor<ReadContext, ReadInfo> recursor, Consumer<Object> setValue) {
		JobOutput<Integer> size = recursor.computeFlat("size", context -> {
			if (sizeField.expectUniform) return (int) decodeUniformInteger(getMinSize(), getMaxSize(), context.input);
			else return (int) decodeVariableInteger(getMinSize(), getMaxSize(), context.input);
		});

		recursor.runNested("elements", nested -> {
			Map<?, ?> map = (Map<?, ?>) constructCollectionWithSize(
					field.type != null ? field.type : HashMap.class, size.get(), recursor.info.sizeLimit
			);

			LegacyMapInstance legacyInstance = nested.info.backwardCompatible ?
					new LegacyMapInstance((HashMap<?, ?>) map) : null;
			for (int counter = 0; counter < size.get(); counter++) {
				DelayedEntry delayed = new DelayedEntry((key, value) -> {
					//noinspection unchecked
					((Map<Object, Object>) map).put(key, value);
				});
				readElement(keysWrapper, nested, key -> {
					if (legacyInstance != null) legacyInstance.legacyKeys.add(key);
					delayed.setKey(key);
				});
				readElement(valuesWrapper, nested, value -> {
					if (legacyInstance != null) legacyInstance.legacyValues.add(value);
					delayed.setValue(value);
				});
			}

			if (nested.info.backwardCompatible) setValue.accept(legacyInstance);
			else setValue.accept(map);
		});
	}

	private void readElement(BitFieldWrapper wrapper, Recursor<ReadContext, ReadInfo> recursor, Consumer<Object> setValue) {
		JobOutput<Boolean> hasValue = recursor.computeFlat("optional", context -> {
			if (wrapper.field.optional) {
				return context.input.read();
			} else return true;
		});

		recursor.runNested("key/value", nested -> {
			if (!hasValue.get()) {
				setValue.accept(null);
				return;
			}

			List<Object> rememberElement = new ArrayList<>(1);
			wrapper.readValue(nested, element -> {
				rememberElement.add(element);
				setValue.accept(element);
			});

			if (wrapper.field.referenceTargetLabel != null) {
				nested.runFlat("referenceTargetLabel", context ->
						context.idLoader.register(
							wrapper.field.referenceTargetLabel,
							rememberElement.get(0), context.input, nested.info.bitser.cache
						)
				);
			}
		});
	}

	private int getMinSize() {
		return (int) max(0, sizeField.minValue);
	}

	private int getMaxSize() {
		return (int) min(Integer.MAX_VALUE, sizeField.maxValue);
	}

	@Override
	void setLegacyValue(Recursor<ReadContext, ReadInfo> recursor, Object rawLegacyMap, Consumer<Object> setValue) {
		if (rawLegacyMap == null) {
			super.setLegacyValue(recursor, null, setValue);
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
			recursor.runNested("keys", nested ->
					keysWrapper.setLegacyValue(nested, legacyKey, delayed::setKey)
			);
			recursor.runNested("values", nested ->
					valuesWrapper.setLegacyValue(nested, legacyValue, delayed::setValue)
			);
		});

		super.setLegacyValue(recursor, legacyInstance.newMap, setValue);
	}

	@Override
	void fixLegacyTypes(Recursor<ReadContext, ReadInfo> recursor, Object rawLegacyInstance) {
		if (rawLegacyInstance == null && field.optional) return;
		assert rawLegacyInstance != null;
		LegacyMapInstance legacyInstance = (LegacyMapInstance) rawLegacyInstance;
		legacyInstance.newMap = (Map<?, ?>) constructCollectionWithSize(
				field.type, legacyInstance.legacyMap.size(), recursor.info.sizeLimit
		);
		if (field.referenceTargetLabel != null) {
			recursor.runFlat("referenceTargetLabel", context ->
					context.idLoader.replace(field.referenceTargetLabel, legacyInstance, legacyInstance.newMap)
			);
		}

		for (Object key : legacyInstance.legacyKeys) {
			recursor.runNested("key", nested ->
					keysWrapper.fixLegacyTypes(nested, key)
			);
		}
		for (Object value : legacyInstance.legacyValues) {
			recursor.runNested("value", nested ->
					valuesWrapper.fixLegacyTypes(nested, value)
			);
		}
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
			if (hasKey) throw new UnexpectedBitserException("Already has key");
			hasKey = true;
			this.key = key;
			maybeInsert();
		}

		void setValue(Object value) {
			if (hasValue) throw new UnexpectedBitserException("Already has value");
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
