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

	@BitField
	private final IntegerField.Properties lengthField;

	StringFieldWrapper(VirtualField field, StringField stringField) {
		super(field);
		long minLength = 0;
		long maxLength = Integer.MAX_VALUE;
		boolean expectUniform = false;
		if (stringField != null) {
			minLength = max(minLength, stringField.length().minValue());
			maxLength = min(maxLength, stringField.length().maxValue());
			expectUniform = stringField.length().expectUniform();
		}
		this.lengthField = new IntegerField.Properties(minLength, maxLength, expectUniform);
	}

	@SuppressWarnings("unused")
	private StringFieldWrapper() {
		super();
		this.lengthField = new IntegerField.Properties();
	}

	@Override
	void writeValue(Object value, WriteJob write) throws IOException {
		String string = (String) value;
		byte[] bytes = string.getBytes(StandardCharsets.UTF_8);

		write.output.prepareProperty("string-length", -1);
		if (lengthField.expectUniform) {
			encodeUniformInteger(bytes.length, lengthField.minValue, lengthField.maxValue, write.output);
		} else encodeVariableInteger(bytes.length, lengthField.minValue, lengthField.maxValue, write.output);
		write.output.finishProperty();

		int counter = 0;
		for (byte b : bytes) {
			write.output.prepareProperty("string-char", counter++);
			encodeUniformInteger(b, Byte.MIN_VALUE, Byte.MAX_VALUE, write.output);
			write.output.finishProperty();
		}
	}

	@Override
	void readValue(ReadJob read, ValueConsumer setValue) throws IOException {
		int length;
		if (lengthField.expectUniform) {
			length = (int) decodeUniformInteger(lengthField.minValue, lengthField.maxValue, read.input);
		} else length = (int) decodeVariableInteger(lengthField.minValue, lengthField.maxValue, read.input);

		byte[] bytes = new byte[length];
		for (int index = 0; index < length; index++) {
			bytes[index] = (byte) decodeUniformInteger(Byte.MIN_VALUE, Byte.MAX_VALUE, read.input);
		}
		setValue.consume(new String(bytes, StandardCharsets.UTF_8));
	}
}
