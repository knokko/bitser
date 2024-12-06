package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.BitserCache;
import com.github.knokko.bitser.util.ReferenceIdLoader;
import com.github.knokko.bitser.util.ReferenceIdMapper;
import com.github.knokko.bitser.util.VirtualField;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.*;

import static com.github.knokko.bitser.serialize.IntegerBitser.*;
import static com.github.knokko.bitser.serialize.IntegerBitser.decodeVariableInteger;
import static java.lang.Math.max;
import static java.lang.Math.min;

class MapFieldWrapper extends BitFieldWrapper {

	static void writeElement(
			Object element, BitFieldWrapper wrapper, BitOutputStream output,
			BitserCache cache, ReferenceIdMapper idMapper, String nullErrorMessage
	) throws IOException {
		if (wrapper.field.optional) output.write(element != null);
		else if (element == null) throw new InvalidBitValueException(nullErrorMessage);
		if (element != null) {
			wrapper.writeValue(element, output, cache, idMapper);
			if (wrapper.field.referenceTargetLabel != null) {
				idMapper.maybeEncodeUnstableId(wrapper.field.referenceTargetLabel, element, output);
			}
		}
	}

	static Object constructCollectionWithSize(VirtualField field, int size) {
		try {
			return field.type.getConstructor(int.class).newInstance(size);
		} catch (NoSuchMethodException noIntConstructor) {
			try {
				return field.type.getConstructor().newInstance();
			} catch (Exception unexpected) {
				throw new RuntimeException(unexpected);
			}
		} catch (Exception unexpected) {
			throw new RuntimeException(unexpected);
		}
	}

	private final IntegerField sizeField;
	private final BitFieldWrapper keysWrapper, valuesWrapper;

	MapFieldWrapper(VirtualField field, IntegerField sizeField, BitFieldWrapper keysWrapper, BitFieldWrapper valuesWrapper) {
		super(field);
		if (sizeField.minValue() > Integer.MAX_VALUE || sizeField.maxValue() < 0) throw new IllegalArgumentException();
		if (field.type.isInterface() || Modifier.isAbstract(field.type.getModifiers())) {
			throw new InvalidBitFieldException("Field type must not be abstract or an interface: " + field);
		}
		this.sizeField = sizeField;
		this.keysWrapper = keysWrapper;
		this.valuesWrapper = valuesWrapper;
	}

	@Override
	void collectReferenceTargetLabels(
			BitserCache cache, Set<String> declaredTargetLabels,
			Set<String> stableLabels, Set<String> unstableLabels, Set<Object> visitedObjects
	) {
		super.collectReferenceTargetLabels(cache, declaredTargetLabels, stableLabels, unstableLabels, visitedObjects);
		keysWrapper.collectReferenceTargetLabels(cache, declaredTargetLabels, stableLabels, unstableLabels, visitedObjects);
		valuesWrapper.collectReferenceTargetLabels(cache, declaredTargetLabels, stableLabels, unstableLabels, visitedObjects);
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
	void writeValue(
			Object rawValue, BitOutputStream output, BitserCache cache, ReferenceIdMapper idMapper
	) throws IOException {
		Map<?, ?> map = (Map<?, ?>) rawValue;
		if (sizeField.expectUniform()) encodeUniformInteger(map.size(), getMinSize(), getMaxSize(), output);
		else encodeVariableInteger(map.size(), getMinSize(), getMaxSize(), output);

		for (Map.Entry<?, ?> entry : map.entrySet()) {
			writeElement(
					entry.getKey(), keysWrapper, output, cache, idMapper,
					"Field " + field + " must not have null keys"
			);
			writeElement(
					entry.getValue(), valuesWrapper, output, cache, idMapper,
					"Field " + field + " must not have null values"
			);
		}
	}

	@Override
	void readValue(
			BitInputStream input, BitserCache cache, ReferenceIdLoader idLoader, ValueConsumer setValue
	) throws IOException {
		int size;
		if (sizeField.expectUniform()) size = (int) decodeUniformInteger(getMinSize(), getMaxSize(), input);
		else size = (int) decodeVariableInteger(getMinSize(), getMaxSize(), input);

		Map<?, ?> map = (Map<?, ?>) constructCollectionWithSize(field, size);
		for (int counter = 0; counter < size; counter++) {
			DelayedEntry delayed = new DelayedEntry(map);
			readElement(keysWrapper, input, cache, idLoader, delayed::setKey);
			readElement(valuesWrapper, input, cache, idLoader, delayed::setValue);
		}
		setValue.consume(map);
	}

	private void readElement(
			BitFieldWrapper wrapper, BitInputStream input, BitserCache cache,
			ReferenceIdLoader idLoader, ValueConsumer setValue
	) throws IOException {
		if (wrapper.field.optional && !input.read()) {
			setValue.consume(null);
		} else {
			List<Object> rememberElement = new ArrayList<>(1);
			wrapper.readValue(input, cache, idLoader, element -> {
				rememberElement.add(element);
				setValue.consume(element);
			});

			if (wrapper.field.referenceTargetLabel != null) {
				idLoader.register(wrapper.field.referenceTargetLabel, rememberElement.get(0), input, cache);
			}
		}
	}

	private int getMinSize() {
		return (int) max(0, sizeField.minValue());
	}

	private int getMaxSize() {
		return (int) min(Integer.MAX_VALUE, sizeField.maxValue());
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
