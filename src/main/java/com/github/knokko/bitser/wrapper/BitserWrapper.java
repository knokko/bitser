package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.BitserCache;
import com.github.knokko.bitser.util.ReferenceIdLoader;
import com.github.knokko.bitser.util.ReferenceIdMapper;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public abstract class BitserWrapper<T> {

	public static <T> BitserWrapper<T> wrap(Class<T> objectClass) {
		BitStruct bitStruct = objectClass.getAnnotation(BitStruct.class);
		if (bitStruct != null) return new BitStructWrapper<T>(objectClass, bitStruct);

		throw new InvalidBitFieldException(objectClass + " is not a BitStruct");
	}

	BitserWrapper() {}

	public abstract void collectReferenceTargetLabels(
			BitserCache cache, Set<String> declaredLabels,
			Set<String> stableLabels, Set<String> unstableLabels, Set<Object> visitedStructs
	);

	public abstract void registerReferenceTargets(Object object, BitserCache cache, ReferenceIdMapper idMapper);

	public abstract UUID getStableId(Object target);

	public abstract void write(
			Object object, BitOutputStream output, BitserCache cache, ReferenceIdMapper idMapper
	) throws IOException;

	public abstract void read(
			BitInputStream input, BitserCache cache, ReferenceIdLoader idLoader, ValueConsumer setValue
	) throws IOException;

	public abstract T shallowCopy(Object original);
}
