package com.github.knokko.bitser.context;

import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.util.ReferenceIdLoader;

import java.util.Objects;

public class ReadContext {

	public final BitInputStream input;
	public final ReferenceIdLoader idLoader;

	public ReadContext(BitInputStream input, ReferenceIdLoader idLoader) {
		this.input = Objects.requireNonNull(input);
		this.idLoader = Objects.requireNonNull(idLoader);
	}
}
