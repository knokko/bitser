package com.github.knokko.bitser.context;

import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.util.ReferenceIdMapper;

import java.util.Objects;

public class WriteContext {

	public final BitOutputStream output;
	public final ReferenceIdMapper idMapper;

	public WriteContext(BitOutputStream output, ReferenceIdMapper idMapper) {
		this.output = Objects.requireNonNull(output);
		this.idMapper = Objects.requireNonNull(idMapper);
	}
}
