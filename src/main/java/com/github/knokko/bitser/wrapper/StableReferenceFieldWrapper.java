package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.BitserCache;
import com.github.knokko.bitser.util.ReferenceIdLoader;
import com.github.knokko.bitser.util.ReferenceIdMapper;

import java.io.IOException;
import java.lang.reflect.Field;

class StableReferenceFieldWrapper extends BitFieldWrapper {

	private final String label;

	StableReferenceFieldWrapper(BitField.Properties properties, Field classField, String label) {
		super(properties, classField);
		this.label = label;
	}

	@Override
	void writeValue(Object value, BitOutputStream output, BitserCache cache, ReferenceIdMapper idMapper) throws IOException, IllegalAccessException {
		idMapper.encodeStableId(label, value, output, cache);
	}

	@Override
	void readValue(BitInputStream input, BitserCache cache, ReferenceIdLoader idLoader, ValueConsumer setValue) throws IOException, IllegalAccessException {
		idLoader.getStable(label, setValue, input);
	}
}
