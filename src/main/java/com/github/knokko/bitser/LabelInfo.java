package com.github.knokko.bitser;

import com.github.knokko.bitser.field.FunctionContext;

class LabelInfo {

	final BitserCache cache;
	final boolean backwardCompatible;
	final FunctionContext functionContext;

	LabelInfo(BitserCache cache, boolean backwardCompatible, FunctionContext functionContext) {
		this.cache = cache;
		this.backwardCompatible = backwardCompatible;
		this.functionContext = functionContext;
	}
}
