package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.BitserCache;
import com.github.knokko.bitser.util.ReferenceIdLoader;
import com.github.knokko.bitser.util.ReferenceIdMapper;

import java.io.IOException;
import java.lang.reflect.Field;

class BooleanFieldWrapper extends BitFieldWrapper {

	BooleanFieldWrapper(BitField.Properties properties, Field classField) {
		super(properties, classField);
	}

	@Override
	void writeValue(
			Object value, BitOutputStream output, BitserCache cache, ReferenceIdMapper idMapper
	) throws IOException, IllegalAccessException {
		output.write((Boolean) value);
	}

	@Override
	void readValue(
			BitInputStream input, BitserCache cache, ReferenceIdLoader idLoader, ValueConsumer setValue
	) throws IOException, IllegalAccessException {
		setValue.consume(input.read());
	}
}
