package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.BitserCache;
import com.github.knokko.bitser.util.ReferenceIdLoader;
import com.github.knokko.bitser.util.ReferenceIdMapper;
import com.github.knokko.bitser.util.VirtualField;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

class BitCollectionFieldWrapper extends AbstractCollectionFieldWrapper {

	private final BitFieldWrapper valuesWrapper;

	BitCollectionFieldWrapper(VirtualField field, IntegerField sizeField, BitFieldWrapper valuesWrapper) {
		super(field, sizeField);
		this.valuesWrapper = valuesWrapper;
	}

	@Override
	public String getFieldName() {
		return valuesWrapper.getFieldName();
	}

	@Override
	public BitFieldWrapper getChildWrapper() {
		return valuesWrapper;
	}

	@Override
	void collectReferenceTargetLabels(
			BitserCache cache, Set<String> declaredTargetLabels,
			Set<String> stableLabels, Set<String> unstableLabels, Set<Object> visitedObjects
	) {
		super.collectReferenceTargetLabels(cache, declaredTargetLabels, stableLabels, unstableLabels, visitedObjects);
		valuesWrapper.collectReferenceTargetLabels(cache, declaredTargetLabels, stableLabels, unstableLabels, visitedObjects);
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
	void writeValue(
			Object value, int size, BitOutputStream output, BitserCache cache, ReferenceIdMapper idMapper
	) throws IOException {
		String nullErrorMessage = "Field " + field + " must not have null elements";
		if (field.type.isArray()) {
			for (int index = 0; index < size; index++) {
				writeElement(Array.get(value, index), valuesWrapper, output, cache, idMapper, nullErrorMessage);
			}
		} else {
			for (Object element : (Collection<?>) value) {
				writeElement(element, valuesWrapper, output, cache, idMapper, nullErrorMessage);
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	void readValue(
			Object value, int size, BitInputStream input, BitserCache cache, ReferenceIdLoader idLoader
	) throws IOException {
		for (int index = 0; index < size; index++) {
			if (valuesWrapper.field.optional && !input.read()) {
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

				if (valuesWrapper.field.referenceTargetLabel != null) {
					idLoader.register(valuesWrapper.field.referenceTargetLabel, rememberElement.get(0), input, cache);
				}
			}
		}
	}
}
