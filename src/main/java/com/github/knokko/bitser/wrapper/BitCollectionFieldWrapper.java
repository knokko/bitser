package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.BitserCache;
import com.github.knokko.bitser.util.ReferenceIdLoader;
import com.github.knokko.bitser.util.ReferenceIdMapper;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

class BitCollectionFieldWrapper extends AbstractCollectionFieldWrapper {

	private final BitFieldWrapper valuesWrapper;

	BitCollectionFieldWrapper(BitField.Properties properties, Field classField, IntegerField sizeField, BitFieldWrapper valuesWrapper) {
		super(properties, sizeField, classField);
		this.valuesWrapper = valuesWrapper;
	}

	@Override
	void collectReferenceTargetLabels(BitserCache cache, Set<String> destination, Set<Object> visitedObjects) {
		super.collectReferenceTargetLabels(cache, destination, visitedObjects);
		valuesWrapper.collectReferenceTargetLabels(cache, destination, visitedObjects);
	}

	@Override
	void registerReferenceTargets(Object value, BitserCache cache, ReferenceIdMapper idMapper) {
		super.registerReferenceTargets(value, cache, idMapper);
		if (value == null) return;
		if (properties.type.isArray()) {
			int size = Array.getLength(value);
			for (int index = 0; index < size; index++) {
				valuesWrapper.registerReferenceTargets(Array.get(value, index), cache, idMapper);
			}
		} else {
			for (Object element : (Collection<?>) value) valuesWrapper.registerReferenceTargets(element, cache, idMapper);
		}
	}

	@Override
	void writeValue(
			Object value, int size, BitOutputStream output, BitserCache cache, ReferenceIdMapper idMapper
	) throws IOException, IllegalAccessException {
		if (properties.type.isArray()) {
			for (int index = 0; index < size; index++) writeElement(Array.get(value, index), output, cache, idMapper);
		} else {
			for (Object element : (Collection<?>) value) writeElement(element, output, cache, idMapper);
		}
	}

	private void writeElement(
			Object element, BitOutputStream output, BitserCache cache, ReferenceIdMapper idMapper
	) throws IOException, IllegalAccessException {
		if (valuesWrapper.properties.optional) output.write(element != null);
		else if (element == null) {
			throw new NullPointerException("Field " + classField + " must not have null values");
		}
		if (element != null) {
			valuesWrapper.writeValue(element, output, cache, idMapper);
			if (valuesWrapper.properties.referenceTarget != null && !valuesWrapper.properties.referenceTarget.stable()) {
				idMapper.encodeUnstableId(valuesWrapper.properties.referenceTarget.label(), element, output);
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	void readValue(
			Object value, int size, BitInputStream input, BitserCache cache, ReferenceIdLoader idLoader
	) throws IOException, IllegalAccessException {
		for (int index = 0; index < size; index++) {
			if (valuesWrapper.properties.optional && !input.read()) {
				if (value instanceof Collection<?>) {
					((Collection<Object>) value).add(null);
				} else {
					Array.set(value, index, null);
				}
			} else {
				final int rememberIndex = index;
				List<Object> rememberElement = new ArrayList<>(1);
				valuesWrapper.readValue(input, cache, idLoader, element -> {
					rememberElement.add(element);
					if (value instanceof Collection<?>) {
						((Collection<Object>) value).add(element);
					} else {
						Array.set(value, rememberIndex, element);
					}
				});

				if (valuesWrapper.properties.referenceTarget != null) {
					Object element = rememberElement.get(0);
					if (valuesWrapper.properties.referenceTarget.stable()) {
						idLoader.registerStable(valuesWrapper.properties.referenceTarget.label(), element, cache);
					} else idLoader.registerUnstable(valuesWrapper.properties.referenceTarget.label(), element, input);
				}
			}
		}
	}
}
