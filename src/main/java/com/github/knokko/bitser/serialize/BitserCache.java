package com.github.knokko.bitser.serialize;

import com.github.knokko.bitser.wrapper.BitserWrapper;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BitserCache {

    private final Map<Class<?>, BitserWrapper<?>> wrappers;

    public BitserCache(boolean threadSafe) {
        if (threadSafe) wrappers = new ConcurrentHashMap<>();
        else wrappers = new HashMap<>();
    }

    public <T> BitserWrapper<T> getWrapper(Class<T> objectClass) {
        //noinspection unchecked
        return (BitserWrapper<T>) wrappers.computeIfAbsent(objectClass, BitserWrapper::wrap);
    }
}
