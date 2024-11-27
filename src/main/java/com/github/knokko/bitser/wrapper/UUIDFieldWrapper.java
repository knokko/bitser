package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.BitserCache;
import com.github.knokko.bitser.util.ReferenceIdLoader;
import com.github.knokko.bitser.util.ReferenceIdMapper;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.UUID;

import static com.github.knokko.bitser.serialize.IntegerBitser.decodeUniformInteger;
import static com.github.knokko.bitser.serialize.IntegerBitser.encodeUniformInteger;

class UUIDFieldWrapper extends BitFieldWrapper {

	final boolean isStableReferenceId;

	UUIDFieldWrapper(BitField.Properties properties, Field classField, boolean isStableReferenceId) {
		super(properties, classField);
		this.isStableReferenceId = isStableReferenceId;
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
	) throws IOException, IllegalAccessException {
		setValue.consume(new UUID(
				decodeUniformInteger(Long.MIN_VALUE, Long.MAX_VALUE, input),
				decodeUniformInteger(Long.MIN_VALUE, Long.MAX_VALUE, input)
		));
	}
}
