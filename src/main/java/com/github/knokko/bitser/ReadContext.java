package com.github.knokko.bitser;

import com.github.knokko.bitser.io.BitInputStream;

import java.util.Objects;

class ReadContext {

	final BitInputStream input;
	final ReferenceIdLoader idLoader;

	ReadContext(BitInputStream input, ReferenceIdLoader idLoader) {
		this.input = Objects.requireNonNull(input);
		this.idLoader = Objects.requireNonNull(idLoader);
	}
}
