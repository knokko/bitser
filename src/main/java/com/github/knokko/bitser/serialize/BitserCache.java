package com.github.knokko.bitser.serialize;

import com.github.knokko.bitser.wrapper.BitStructWrapper;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BitserCache {

	private final Map<Class<?>, BitStructWrapper<?>> wrappers;

	public BitserCache(boolean threadSafe) {
		if (threadSafe) wrappers = new ConcurrentHashMap<>();
		else wrappers = new HashMap<>();
	}

	public <T> BitStructWrapper<T> getWrapper(Class<T> objectClass) {
		//noinspection unchecked
		return (BitStructWrapper<T>) wrappers.computeIfAbsent(objectClass, BitStructWrapper::wrap);
	}
}
