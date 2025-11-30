package com.github.knokko.bitser.context;

import com.github.knokko.bitser.field.FunctionContext;
import com.github.knokko.bitser.serialize.BitserCache;

import java.util.Objects;

public class LegacyInfo {

	public final BitserCache cache;
	public final FunctionContext functionContext;

	public LegacyInfo(BitserCache cache, FunctionContext functionContext) {
		this.cache = Objects.requireNonNull(cache);
		this.functionContext = Objects.requireNonNull(functionContext);
	}
}
