package com.github.knokko.bitser.field;

import java.util.Map;

public class FunctionContext {

	public final Map<String, Object> withParameters;

	public FunctionContext(Map<String, Object> withParameters) {
		this.withParameters = withParameters;
	}
}
