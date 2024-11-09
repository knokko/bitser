package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.BitserCache;

import java.io.IOException;
import java.lang.reflect.Field;

class BooleanFieldWrapper extends BitFieldWrapper {

	BooleanFieldWrapper(BitField.Properties properties, Field classField) {
		super(properties, classField);
	}

	@Override
	void writeValue(Object value, BitOutputStream output, BitserCache cache) throws IOException, IllegalAccessException {
		output.write((Boolean) value);
	}

	@Override
	Object readValue(BitInputStream input, BitserCache cache) throws IOException, IllegalAccessException {
		return input.read();
	}
}
