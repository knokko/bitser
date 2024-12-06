package com.github.knokko.bitser.io;

import com.github.knokko.bitser.serialize.Bitser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class BitserHelper {

	@SuppressWarnings("unchecked")
	public static <T> T serializeAndDeserialize(Bitser bitser, T object, Object... with) throws IOException {
		ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
		BitOutputStream bitOutput = new BitOutputStream(byteOutput);
		bitser.serialize(object, bitOutput, with);
		bitOutput.finish();
		return (T) bitser.deserialize(object.getClass(), new BitInputStream(new ByteArrayInputStream(byteOutput.toByteArray())), with);
	}
}
