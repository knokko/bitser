package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.StringField;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.BitserCache;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

import static com.github.knokko.bitser.serialize.IntegerBitser.*;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class StringFieldWrapper extends BitFieldWrapper {

	private final StringField stringField;

	StringFieldWrapper(BitField bitField, StringField stringField, Field classField) {
		super(bitField, classField);
		this.stringField = stringField;
	}

	@Override
	void writeValue(Object value, BitOutputStream output, BitserCache cache) throws IOException {
		if (stringField.optional()) output.write(value != null);
		if (stringField.optional() && value == null) return;
		if (value == null) throw new NullPointerException(classField + " must not be null");

		String string = (String) value;
		byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
		if (stringField.length().expectUniform()) encodeUniformInteger(bytes.length, minLength(), maxLength(), output);
		else encodeVariableInteger(bytes.length, minLength(), maxLength(), output);

		for (byte b : bytes) encodeUniformInteger(b, Byte.MIN_VALUE, Byte.MAX_VALUE, output);
	}

	@Override
	Object readValue(BitInputStream input, BitserCache cache) throws IOException {
		if (stringField.optional() && !input.read()) return null;

		int length;
		if (stringField.length().expectUniform()) length = (int) decodeUniformInteger(minLength(), maxLength(), input);
		else length = (int) decodeVariableInteger(minLength(), maxLength(), input);

		byte[] bytes = new byte[length];
		for (int index = 0; index < length; index++) {
			bytes[index] = (byte) decodeUniformInteger(Byte.MIN_VALUE, Byte.MAX_VALUE, input);
		}
		return new String(bytes, StandardCharsets.UTF_8);
	}

	private int minLength() {
		return (int) max(0, stringField.length().minValue());
	}

	private int maxLength() {
		return (int) min(Integer.MAX_VALUE, stringField.length().maxValue());
	}
}
