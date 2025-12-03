package com.github.knokko.bitser;

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
		//noinspection unchecked
		return (BitStructWrapper<T>) wrappers.computeIfAbsent(objectClass, BitStructWrapper::wrap);
	}
}
