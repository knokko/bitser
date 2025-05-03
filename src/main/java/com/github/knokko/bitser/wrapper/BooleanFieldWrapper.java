package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.serialize.ReadJob;
import com.github.knokko.bitser.serialize.WriteJob;
import com.github.knokko.bitser.util.VirtualField;

import java.io.IOException;

@BitStruct(backwardCompatible = false)
class BooleanFieldWrapper extends BitFieldWrapper {

	BooleanFieldWrapper(VirtualField field) {
		super(field);
	}

	@SuppressWarnings("unused")
	private BooleanFieldWrapper() {
		super();
	}

	@Override
	void writeValue(Object value, WriteJob write) throws IOException {
		write.output.prepareProperty("boolean-value", -1);
		write.output.write((Boolean) value);
		write.output.finishProperty();
	}

	@Override
	void readValue(ReadJob read, ValueConsumer setValue) throws IOException {
		setValue.consume(read.input.read());
	}
}
