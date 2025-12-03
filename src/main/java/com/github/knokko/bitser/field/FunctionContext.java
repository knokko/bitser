package com.github.knokko.bitser.field;

import com.github.knokko.bitser.Bitser;

import java.util.Map;
import java.util.Objects;

public class FunctionContext {

	public final Bitser bitser;
	public final Map<String, Object> withParameters;

	public FunctionContext(Bitser bitser, Map<String, Object> withParameters) {
		this.bitser = Objects.requireNonNull(bitser);
		this.withParameters = withParameters;
	}
}
