package com.github.knokko.bitser.wrapper;

import java.io.IOException;

@FunctionalInterface
public interface ValueConsumer {

	void consume(Object value) throws IOException, IllegalAccessException;
}
