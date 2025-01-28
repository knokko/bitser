package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.field.StringField;
import com.github.knokko.bitser.serialize.ReadJob;
import com.github.knokko.bitser.serialize.WriteJob;
import com.github.knokko.bitser.util.VirtualField;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.github.knokko.bitser.serialize.IntegerBitser.*;
import static java.lang.Math.max;
import static java.lang.Math.min;

@BitStruct(backwardCompatible = false)
public class StringFieldWrapper extends BitFieldWrapper {

	@BitField(optional = true)
	private final IntegerField.Properties lengthField;

	StringFieldWrapper(VirtualField field, StringField stringField) {
		super(field);
		this.lengthField = stringField != null ? new IntegerField.Properties(stringField.length()) : null;
	}

	@Override
	void writeValue(Object value, WriteJob write) throws IOException {
		String string = (String) value;
		byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
		if (lengthField != null && lengthField.expectUniform) {
			encodeUniformInteger(bytes.length, minLength(), maxLength(), write.output);
		} else encodeVariableInteger(bytes.length, minLength(), maxLength(), write.output);

		for (byte b : bytes) encodeUniformInteger(b, Byte.MIN_VALUE, Byte.MAX_VALUE, write.output);
	}

	@Override
	void readValue(ReadJob read, ValueConsumer setValue) throws IOException {
		int length;
		if (lengthField != null && lengthField.expectUniform) {
			length = (int) decodeUniformInteger(minLength(), maxLength(), read.input);
		} else length = (int) decodeVariableInteger(minLength(), maxLength(), read.input);

		byte[] bytes = new byte[length];
		for (int index = 0; index < length; index++) {
			bytes[index] = (byte) decodeUniformInteger(Byte.MIN_VALUE, Byte.MAX_VALUE, read.input);
		}
		setValue.consume(new String(bytes, StandardCharsets.UTF_8));
	}

	private int minLength() {
		if (lengthField == null) return 0;
		return (int) max(0, lengthField.minValue);
	}

	private int maxLength() {
		if (lengthField == null) return Integer.MAX_VALUE;
		return (int) min(Integer.MAX_VALUE, lengthField.maxValue);
	}
}
