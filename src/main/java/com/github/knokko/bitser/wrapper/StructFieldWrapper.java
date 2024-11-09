package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.BitserCache;

import java.io.IOException;
import java.lang.reflect.Field;

class StructFieldWrapper extends BitFieldWrapper {

	StructFieldWrapper(BitField.Properties properties, Field classField) {
		super(properties, classField);
	}

	@Override
	void writeValue(Object value, BitOutputStream output, BitserCache cache) throws IOException {
		if (value != null) cache.getWrapper(properties.type).write(value, output, cache);
	}

	@Override
	Object readValue(BitInputStream input, BitserCache cache) throws IOException {
		return cache.getWrapper(properties.type).read(input, cache);
	}
}
