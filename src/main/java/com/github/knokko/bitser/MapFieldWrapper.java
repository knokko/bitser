package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.exceptions.LegacyBitserException;
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
import static com.github.knokko.bitser.AbstractCollectionFieldWrapper.constructCollectionWithSize;
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
		this.sizeField = new IntegerField.Properties(
				max(0, sizeField.minValue()), min(Integer.MAX_VALUE, sizeField.maxValue()),
				sizeField.expectUniform(), sizeField.digitSize(), sizeField.commonValues()
		);
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
		recursor.runNested("keys", nested ->
				((Map<?, ?>) map).forEach((key, value) ->
						keysWrapper.registerLegacyClasses(key, nested)
				)
		);
		recursor.runNested("values", nested ->
				((Map<?, ?>) map).forEach((key, value) ->
						valuesWrapper.registerLegacyClasses(value, nested)
				)
		);
	}

	@Override
	void collectReferenceLabels(Recursor<LabelContext, LabelInfo> recursor) {
		super.collectReferenceLabels(recursor);
		recursor.runNested("keys", keysWrapper::collectReferenceLabels);
		recursor.runNested("values", valuesWrapper::collectReferenceLabels);
	}

	@Override
	void registerReferenceTargets(Object rawValue, Recursor<ReferenceIdMapper, BitserCache> recursor) {
		super.registerReferenceTargets(rawValue, recursor);
		if (rawValue == null) return;
		Map<?, ?> map = (Map<?, ?>) rawValue;
		if (map.isEmpty()) return;
		recursor.runNested("keys", nested -> {
			for (Map.Entry<?, ?> entry : map.entrySet()) {
				keysWrapper.registerReferenceTargets(entry.getKey(), nested);
			}
		});
		recursor.runNested("values", nested -> {
			for (Map.Entry<?, ?> entry : map.entrySet()) {
				valuesWrapper.registerReferenceTargets(entry.getValue(), nested);
			}
		});
	}

	private void writeElementSet(
			Recursor<WriteContext, WriteInfo> recursor,
			BitFieldWrapper elementWrapper,
			Collection<?> elementSet,
			String keyOrValue,
			String nullErrorMessage
	) {
		if (elementWrapper.field.optional) {
			recursor.runFlat("optional", context -> {
				context.output.prepareProperty("optional", -1);
				for (Object element : elementSet) {
					context.output.write(element != null);
				}
				context.output.finishProperty();
			});
		}

		if (recursor.info.usesContextInfo) {
			int index = 0;
			for (Object element : elementSet) {
				if (element == null) {
					if (!elementWrapper.field.optional) throw new InvalidBitValueException(nullErrorMessage);
					continue;
				}

				final int rememberIndex = index;
				recursor.runFlat("pushContext", context ->
						context.output.pushContext(keyOrValue, rememberIndex)
				);
				recursor.runNested(keyOrValue + " " + index, deeper ->
						elementWrapper.writeValue(element, deeper)
				);
				recursor.runFlat("popContext", context ->
						context.output.popContext(keyOrValue, rememberIndex)
				);
				index += 1;
			}
		} else {
			for (Object element : elementSet) {
				if (element == null) {
					if (!elementWrapper.field.optional) throw new InvalidBitValueException(nullErrorMessage);
					continue;
				}

				elementWrapper.writeValue(element, recursor);
			}
		}

		if (elementWrapper.field.referenceTargetLabel != null) {
			recursor.runFlat("referenceTargetLabel", context -> {
				context.output.pushContext("reference-target-label", -1);
				int counter = -1;
				for (Object element : elementSet) {
					counter += 1;
					if (element == null) continue;
					context.idMapper.maybeEncodeUnstableId(
							elementWrapper.field.referenceTargetLabel, element, context.output, counter
					);
				}
				context.output.popContext("reference-target-label", -1);
			});
		}
	}

	@Override
	void writeValue(Object rawValue, Recursor<WriteContext, WriteInfo> recursor) {
		Map<?, ?> map = (Map<?, ?>) rawValue;
		recursor.runFlat("size", context -> {
			if (context.integerDistribution != null) {
				context.integerDistribution.insert(field + " map size", (long) map.size(), sizeField);
				context.integerDistribution.insert("map size", (long) map.size(), sizeField);
			}
			context.output.prepareProperty("map-size", -1);
			encodeInteger(map.size(), sizeField, context.output);
			context.output.finishProperty();
		});

		if (map.isEmpty()) return;
		recursor.runNested("keys", nested -> writeElementSet(
				nested, keysWrapper, map.keySet(), "key",
				"Field " + field + " must not have null keys"
		));
		recursor.runNested("values", nested -> writeElementSet(
				nested, valuesWrapper, map.values(), "value",
				"Field " + field + " must not have null values"
		));
	}

	private void readElementSet(
			Recursor<ReadContext, ReadInfo> recursor,
			RawMap raw, BitFieldWrapper elementWrapper
	) {
		boolean readKeys = elementWrapper == keysWrapper;

		JobOutput<boolean[]> hasElements = null;
		if (elementWrapper.field.optional) {
			hasElements = recursor.computeFlat("optional", context -> {
				boolean[] hadElements = new boolean[raw.delayed.length];
				for (int index = 0; index < raw.delayed.length; index++) {
					hadElements[index] = context.input.read();
				}
				return hadElements;
			});
		}

		JobOutput<boolean[]> finalHasElements = hasElements;
		recursor.runNested("elements", nested -> {
			for (int index = 0; index < raw.delayed.length; index++) {
				DelayedEntry delayed = raw.delayed[index];
				if (finalHasElements == null || finalHasElements.get()[index]) {
					elementWrapper.readValue(nested, element -> {
						if (readKeys) {
							if (raw.legacy != null) raw.legacy.legacyKeys.add(element);
							delayed.setKey(element);
						} else {
							if (raw.legacy != null) raw.legacy.legacyValues.add(element);
							delayed.setValue(element);
						}
					});
				} else {
					if (readKeys) {
						if (raw.legacy != null) raw.legacy.legacyKeys.add(null);
						delayed.setKey(null);
					} else {
						if (raw.legacy != null) raw.legacy.legacyValues.add(null);
						delayed.setValue(null);
					}
				}
			}
		});

		if (elementWrapper.field.referenceTargetLabel != null) {
			recursor.runFlat("referenceTargetLabel", context -> {
				for (int index = 0; index < raw.delayed.length; index++) {
					if (finalHasElements != null && !finalHasElements.get()[index]) continue;
					DelayedEntry delayed = raw.delayed[index];
					context.idLoader.register(
							elementWrapper.field.referenceTargetLabel,
							readKeys ? delayed.key : delayed.value, context.input, recursor.info.bitser.cache
					);
				}
			});
		}
	}

	@Override
	void readValue(Recursor<ReadContext, ReadInfo> recursor, Consumer<Object> setValue) {

		JobOutput<RawMap> getMap = recursor.computeFlat("size", context -> {
			int size = decodeLength(sizeField, recursor.info.sizeLimit, "map size", context.input);
			RawMap raw = new RawMap();
			raw.map = (Map<?, ?>) constructCollectionWithSize(field.type != null ? field.type : HashMap.class, size);
			raw.legacy = recursor.info.backwardCompatible ?
					new LegacyMapInstance((HashMap<?, ?>) raw.map) : null;
			raw.delayed = new DelayedEntry[size];
			for (int index = 0; index < size; index++) {
				raw.delayed[index] = new DelayedEntry((key, value) -> {
					//noinspection unchecked
					((Map<Object, Object>) raw.map).put(key, value);
				});
			}
			return raw;
		});

		recursor.runNested("keys", nested -> {
			RawMap raw = getMap.get();
			if (raw.delayed.length > 0) {
				readElementSet(nested, raw, keysWrapper);
				readElementSet(nested, raw, valuesWrapper);
			}
		});

		recursor.runFlat("finalize", context -> {
			if (recursor.info.backwardCompatible) setValue.accept(getMap.get().legacy);
			else setValue.accept(getMap.get().map);
		});
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

		DelayedEntry[] delayed = new DelayedEntry[legacyInstance.legacyMap.size()];
		for (int index = 0; index < delayed.length; index++) {
			delayed[index] = new DelayedEntry((newKey, newValue) -> {
				try {
					Array.set(dummyKeyArray, 0, newKey);
				} catch (IllegalArgumentException wrongType) {
					throw new LegacyBitserException("Can't convert from legacy " + newKey + " to " + keysWrapper.field.type + " for field " + field);
				}
				try {
					Array.set(dummyValueArray, 0, newValue);
				} catch (IllegalArgumentException wrongType) {
					throw new LegacyBitserException("Can't convert from legacy " + newValue + " to " + valuesWrapper.field.type + " for field " + field);
				}

				//noinspection unchecked
				((Map<Object, Object>)legacyInstance.newMap).put(newKey, newValue);
			});
		}

		recursor.runNested("keys", nested -> {
			int index = 0;
			for (Object legacyKey : legacyInstance.legacyMap.keySet()) {
				keysWrapper.setLegacyValue(nested, legacyKey, delayed[index]::setKey);
				index += 1;
			}
		});
		recursor.runNested("values", nested -> {
			int index = 0;
			for (Object legacyValue : legacyInstance.legacyMap.values()) {
				valuesWrapper.setLegacyValue(nested, legacyValue, delayed[index]::setValue);
				index += 1;
			}
		});

		super.setLegacyValue(recursor, legacyInstance.newMap, setValue);
	}

	@Override
	void fixLegacyTypes(Recursor<ReadContext, ReadInfo> recursor, Object rawLegacyInstance) {
		if (rawLegacyInstance == null && field.optional) return;
		assert rawLegacyInstance != null;
		LegacyMapInstance legacyInstance = (LegacyMapInstance) rawLegacyInstance;
		legacyInstance.newMap = (Map<?, ?>) constructCollectionWithSize(
				field.type, legacyInstance.legacyMap.size()
		);
		if (field.referenceTargetLabel != null) {
			recursor.runFlat("referenceTargetLabel", context ->
					context.idLoader.replace(field.referenceTargetLabel, legacyInstance, legacyInstance.newMap)
			);
		}

		recursor.runNested("keys", nested -> {
			for (Object key : legacyInstance.legacyKeys) keysWrapper.fixLegacyTypes(nested, key);
		});
		recursor.runNested("values", nested -> {
			for (Object value : legacyInstance.legacyValues) valuesWrapper.fixLegacyTypes(nested, value);
		});
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

	private static class RawMap {
		Map<?, ?> map;
		LegacyMapInstance legacy;
		DelayedEntry[] delayed;
	}
}
