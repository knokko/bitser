package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.field.StringField;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.BitserCache;
import com.github.knokko.bitser.util.ReferenceIdLoader;
import com.github.knokko.bitser.util.ReferenceIdMapper;
import com.github.knokko.bitser.util.VirtualField;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.github.knokko.bitser.serialize.IntegerBitser.*;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class StringFieldWrapper extends BitFieldWrapper {

	private final StringField stringField;

	StringFieldWrapper(VirtualField field, StringField stringField) {
		super(field);
		this.stringField = stringField;
	}

	@Override
	void writeValue(Object value, BitOutputStream output, BitserCache cache, ReferenceIdMapper idMapper) throws IOException {
		String string = (String) value;
		byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
		if (stringField != null && stringField.length().expectUniform()) {
			encodeUniformInteger(bytes.length, minLength(), maxLength(), output);
		} else encodeVariableInteger(bytes.length, minLength(), maxLength(), output);

		for (byte b : bytes) encodeUniformInteger(b, Byte.MIN_VALUE, Byte.MAX_VALUE, output);
	}

	@Override
	void readValue(
			BitInputStream input, BitserCache cache, ReferenceIdLoader idLoader, ValueConsumer setValue
	) throws IOException {
		int length;
		if (stringField != null && stringField.length().expectUniform()) {
			length = (int) decodeUniformInteger(minLength(), maxLength(), input);
		} else length = (int) decodeVariableInteger(minLength(), maxLength(), input);

		byte[] bytes = new byte[length];
		for (int index = 0; index < length; index++) {
			bytes[index] = (byte) decodeUniformInteger(Byte.MIN_VALUE, Byte.MAX_VALUE, input);
		}
		setValue.consume(new String(bytes, StandardCharsets.UTF_8));
	}

	private int minLength() {
		if (stringField == null) return 0;
		return (int) max(0, stringField.length().minValue());
	}

	private int maxLength() {
		if (stringField == null) return Integer.MAX_VALUE;
		return (int) min(Integer.MAX_VALUE, stringField.length().maxValue());
	}
}
