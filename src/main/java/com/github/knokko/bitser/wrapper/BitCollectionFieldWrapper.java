package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.BitserCache;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Collection;

class BitCollectionFieldWrapper extends AbstractCollectionFieldWrapper {

	private final BitFieldWrapper valuesWrapper;

	BitCollectionFieldWrapper(BitField.Properties properties, Field classField, IntegerField sizeField, BitFieldWrapper valuesWrapper) {
		super(properties, sizeField, classField);
		this.valuesWrapper = valuesWrapper;
	}

	@Override
	void writeValue(Object value, int size, BitOutputStream output, BitserCache cache) throws IOException, IllegalAccessException {
		if (properties.type.isArray()) {
			for (int index = 0; index < size; index++) writeElement(Array.get(value, index), output, cache);
		} else {
			for (Object element : (Collection<?>) value) writeElement(element, output, cache);
		}
	}

	private void writeElement(
			Object element, BitOutputStream output, BitserCache cache
	) throws IOException, IllegalAccessException {
		if (valuesWrapper.properties.optional) output.write(element != null);
		else if (element == null) {
			throw new NullPointerException("Field " + classField + " must not have null values");
		}
		if (element != null) valuesWrapper.writeValue(element, output, cache);
	}

	@Override
	@SuppressWarnings("unchecked")
	void readValue(Object value, int size, BitInputStream input, BitserCache cache) throws IOException, IllegalAccessException {
		for (int index = 0; index < size; index++) {
			Object element = null;
			if (!valuesWrapper.properties.optional || input.read()) element = valuesWrapper.readValue(input, cache);
			if (value instanceof Collection<?>) {
				((Collection<Object>) value).add(element);
			} else {
				Array.set(value, index, element);
			}
		}
	}
}
