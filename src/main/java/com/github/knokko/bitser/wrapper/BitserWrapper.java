package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.BitserCache;

import java.io.IOException;

public abstract class BitserWrapper<T> {

	public static <T> BitserWrapper<T> wrap(Class<T> objectClass) {
		BitStruct bitStruct = objectClass.getAnnotation(BitStruct.class);
		if (bitStruct != null) return new BitStructWrapper<T>(objectClass, bitStruct);

		throw new UnsupportedOperationException("Can't serialize " + objectClass);
	}

	BitserWrapper() {
	}

	public abstract void write(Object object, BitOutputStream output, BitserCache cache) throws IOException;

	public abstract T read(BitInputStream input, BitserCache cache) throws IOException;
}
