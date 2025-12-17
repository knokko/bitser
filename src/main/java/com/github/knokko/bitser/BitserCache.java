package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.InvalidBitFieldException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class BitserCache {

	private final Map<Class<?>, BitStructWrapper<?>> wrappers;

	BitserCache(boolean threadSafe) {
		if (threadSafe) wrappers = new ConcurrentHashMap<>();
		else wrappers = new HashMap<>();
	}

	<T> BitStructWrapper<T> getWrapper(Class<T> objectClass) {
		BitStructWrapper<T> result = getWrapperOrNull(objectClass);
		if (result == null) throw new InvalidBitFieldException(objectClass + " is not a BitStruct");
		return result;
	}

	<T> BitStructWrapper<T> getWrapperOrNull(Class<T> objectClass) {
		//noinspection unchecked
		return (BitStructWrapper<T>) wrappers.computeIfAbsent(objectClass, BitStructWrapper::wrap);
	}
}
