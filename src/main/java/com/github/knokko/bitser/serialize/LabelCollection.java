package com.github.knokko.bitser.serialize;

import com.github.knokko.bitser.field.FunctionContext;
import com.github.knokko.bitser.wrapper.BitserWrapper;

import java.util.HashSet;
import java.util.Set;

public class LabelCollection {

	public final BitserCache cache;
	public final Set<String> declaredTargets, stable, unstable;
	public final Set<BitserWrapper<?>> visitedStructs;
	public final boolean backwardCompatible;
	public final FunctionContext functionContext;

	public LabelCollection(
			BitserCache cache, Set<String> declaredTargetLabels,
			boolean backwardCompatible, FunctionContext functionContext
	) {
		this.cache = cache;
		this.declaredTargets = declaredTargetLabels;
		this.stable = new HashSet<>();
		this.unstable = new HashSet<>();
		this.visitedStructs = new HashSet<>();
		this.backwardCompatible = backwardCompatible;
		this.functionContext = functionContext;
	}
}
