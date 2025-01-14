package com.github.knokko.bitser.io;

import com.github.knokko.bitser.serialize.Bitser;

import java.io.IOException;

public class BitserHelper {

	@SuppressWarnings("unchecked")
	public static <T> T serializeAndDeserialize(Bitser bitser, T object, Object... with) throws IOException {
		return (T) bitser.deserializeFromBytes(object.getClass(), bitser.serializeToBytes(object, with), with);
	}
}
