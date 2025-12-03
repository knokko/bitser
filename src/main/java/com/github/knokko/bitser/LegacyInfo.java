package com.github.knokko.bitser;

import com.github.knokko.bitser.field.FunctionContext;

import java.util.Objects;

class LegacyInfo {

	final BitserCache cache;
	final FunctionContext functionContext;

	LegacyInfo(BitserCache cache, FunctionContext functionContext) {
		this.cache = Objects.requireNonNull(cache);
		this.functionContext = Objects.requireNonNull(functionContext);
	}
}
