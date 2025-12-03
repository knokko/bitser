package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.field.StringField;
import com.github.knokko.bitser.util.Recursor;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import static com.github.knokko.bitser.IntegerBitser.*;
import static java.lang.Math.max;
import static java.lang.Math.min;

@BitStruct(backwardCompatible = false)
class StringFieldWrapper extends BitFieldWrapper {

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
	void writeValue(Object value, Recursor<WriteContext, WriteInfo> recursor) {
		String string = (String) value;
		byte[] bytes = string.getBytes(StandardCharsets.UTF_8);

		recursor.runFlat("string", context -> {
			context.output.prepareProperty("string-length", -1);
			if (lengthField.expectUniform) {
				encodeUniformInteger(bytes.length, lengthField.minValue, lengthField.maxValue, context.output);
			} else encodeVariableInteger(bytes.length, lengthField.minValue, lengthField.maxValue, context.output);
			context.output.finishProperty();

			int counter = 0;
			for (byte b : bytes) {
				context.output.prepareProperty("string-char", counter++);
				encodeUniformInteger(b, Byte.MIN_VALUE, Byte.MAX_VALUE, context.output);
				context.output.finishProperty();
			}
		});
	}

	@Override
	void readValue(Recursor<ReadContext, ReadInfo> recursor, Consumer<Object> setValue) {
		recursor.runFlat("string-value", context -> {
			int length;
			if (lengthField.expectUniform) {
				length = (int) decodeUniformInteger(lengthField.minValue, lengthField.maxValue, context.input);
			} else length = (int) decodeVariableInteger(lengthField.minValue, lengthField.maxValue, context.input);

			if (recursor.info.sizeLimit != null && length > recursor.info.sizeLimit.maxSize) {
				throw new InvalidBitValueException(
						"String length of " + length + " exceeds the limit of " + recursor.info.sizeLimit.maxSize
				);
			}

			byte[] bytes = new byte[length];
			for (int index = 0; index < length; index++) {
				bytes[index] = (byte) decodeUniformInteger(Byte.MIN_VALUE, Byte.MAX_VALUE, context.input);
			}
			setValue.accept(new String(bytes, StandardCharsets.UTF_8));
		});
	}
}
