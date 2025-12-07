package com.github.knokko.bitser;

import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.options.CollectionSizeLimit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.github.knokko.bitser.IntegerBitser.*;

public class StringBitser {

	public static void encode(
			String value, IntegerField.Properties lengthField, BitOutputStream output
	) throws IOException {
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);

		output.prepareProperty("string-length", -1);
		encodeInteger(bytes.length, lengthField, output);
		output.finishProperty();

		int counter = 0;
		for (byte b : bytes) {
			output.prepareProperty("string-char", counter++);
			encodeUniformInteger(b, Byte.MIN_VALUE, Byte.MAX_VALUE, output);
			output.finishProperty();
		}
	}

	public static String decode(
			IntegerField.Properties lengthField, CollectionSizeLimit sizeLimit, BitInputStream input
	) throws IOException {
		int length = decodeLength(lengthField, sizeLimit, "string length", input);

		byte[] bytes = new byte[length];
		for (int index = 0; index < length; index++) {
			bytes[index] = (byte) decodeUniformInteger(Byte.MIN_VALUE, Byte.MAX_VALUE, input);
		}
		return new String(bytes, StandardCharsets.UTF_8);
	}
}
