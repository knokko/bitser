package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.BitserCache;
import com.github.knokko.bitser.util.ReferenceIdLoader;
import com.github.knokko.bitser.util.ReferenceIdMapper;
import com.github.knokko.bitser.util.VirtualField;

import java.io.IOException;

class BooleanFieldWrapper extends BitFieldWrapper {

	BooleanFieldWrapper(VirtualField field) {
		super(field);
	}

	@Override
	void writeValue(
			Object value, BitOutputStream output, BitserCache cache, ReferenceIdMapper idMapper
	) throws IOException {
		output.write((Boolean) value);
	}

	@Override
	void readValue(
			BitInputStream input, BitserCache cache, ReferenceIdLoader idLoader, ValueConsumer setValue
	) throws IOException {
		setValue.consume(input.read());
	}
}
