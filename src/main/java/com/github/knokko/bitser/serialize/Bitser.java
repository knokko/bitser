package com.github.knokko.bitser.serialize;

import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;

import java.io.IOException;

public class Bitser {

    private final BitserCache cache;

    public Bitser(boolean threadSafe) {
        this.cache = new BitserCache(threadSafe);
    }

    public void serialize(Object object, BitOutputStream output) throws IOException {
        cache.getWrapper(object.getClass()).write(object, output, cache);
    }

    public <T> T deserialize(Class<T> objectClass, BitInputStream input) throws IOException {
        return cache.getWrapper(objectClass).read(input, cache);
    }
}
