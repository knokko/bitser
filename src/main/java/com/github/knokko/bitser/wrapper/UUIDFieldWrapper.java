package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.field.StableReferenceFieldId;
import com.github.knokko.bitser.serialize.ReadJob;
import com.github.knokko.bitser.serialize.WriteJob;
import com.github.knokko.bitser.util.VirtualField;

import java.io.IOException;
import java.util.UUID;

import static com.github.knokko.bitser.serialize.IntegerBitser.decodeUniformInteger;
import static com.github.knokko.bitser.serialize.IntegerBitser.encodeUniformInteger;

class UUIDFieldWrapper extends BitFieldWrapper {

	final boolean isStableReferenceId;

	UUIDFieldWrapper(VirtualField field) {
		super(field);
		this.isStableReferenceId = field.annotations.has(StableReferenceFieldId.class);
	}

	@Override
	void writeValue(Object rawValue, WriteJob write) throws IOException {
		UUID value = (UUID) rawValue;
		encodeUniformInteger(value.getMostSignificantBits(), Long.MIN_VALUE, Long.MAX_VALUE, write.output);
		encodeUniformInteger(value.getLeastSignificantBits(), Long.MIN_VALUE, Long.MAX_VALUE, write.output);
	}

	@Override
	void readValue(ReadJob read, ValueConsumer setValue) throws IOException {
		setValue.consume(new UUID(
				decodeUniformInteger(Long.MIN_VALUE, Long.MAX_VALUE, read.input),
				decodeUniformInteger(Long.MIN_VALUE, Long.MAX_VALUE, read.input)
		));
	}
}
