package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.BitserCache;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.UUID;

import static com.github.knokko.bitser.serialize.IntegerBitser.decodeUniformInteger;
import static com.github.knokko.bitser.serialize.IntegerBitser.encodeUniformInteger;

public class UUIDFieldWrapper extends BitFieldWrapper {

	UUIDFieldWrapper(BitField.Properties properties, Field classField) {
		super(properties, classField);
	}

	@Override
	void writeValue(Object rawValue, BitOutputStream output, BitserCache cache) throws IOException {
		UUID value = (UUID) rawValue;
		encodeUniformInteger(value.getMostSignificantBits(), Long.MIN_VALUE, Long.MAX_VALUE, output);
		encodeUniformInteger(value.getLeastSignificantBits(), Long.MIN_VALUE, Long.MAX_VALUE, output);
	}

	@Override
	Object readValue(BitInputStream input, BitserCache cache) throws IOException {
		return new UUID(
				decodeUniformInteger(Long.MIN_VALUE, Long.MAX_VALUE, input),
				decodeUniformInteger(Long.MIN_VALUE, Long.MAX_VALUE, input)
		);
	}
}
