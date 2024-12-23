package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.field.StableReferenceFieldId;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.BitserCache;
import com.github.knokko.bitser.util.ReferenceIdLoader;
import com.github.knokko.bitser.util.ReferenceIdMapper;
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
	void writeValue(Object rawValue, BitOutputStream output, BitserCache cache, ReferenceIdMapper idMapper) throws IOException {
		UUID value = (UUID) rawValue;
		encodeUniformInteger(value.getMostSignificantBits(), Long.MIN_VALUE, Long.MAX_VALUE, output);
		encodeUniformInteger(value.getLeastSignificantBits(), Long.MIN_VALUE, Long.MAX_VALUE, output);
	}

	@Override
	void readValue(
			BitInputStream input, BitserCache cache, ReferenceIdLoader idLoader, ValueConsumer setValue
	) throws IOException {
		setValue.consume(new UUID(
				decodeUniformInteger(Long.MIN_VALUE, Long.MAX_VALUE, input),
				decodeUniformInteger(Long.MIN_VALUE, Long.MAX_VALUE, input)
		));
	}
}
