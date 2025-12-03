package com.github.knokko.bitser;

import com.github.knokko.bitser.io.BitOutputStream;

import java.util.Objects;

class WriteContext {

	final BitOutputStream output;
	final ReferenceIdMapper idMapper;

	WriteContext(BitOutputStream output, ReferenceIdMapper idMapper) {
		this.output = Objects.requireNonNull(output);
		this.idMapper = Objects.requireNonNull(idMapper);
	}
}
