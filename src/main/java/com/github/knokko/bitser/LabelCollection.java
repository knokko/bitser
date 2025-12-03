package com.github.knokko.bitser;

import com.github.knokko.bitser.field.FunctionContext;

import java.util.HashSet;
import java.util.Set;

class LabelCollection {

	final BitserCache cache;
	final Set<String> declaredTargets, stable, unstable;
	final Set<BitStructWrapper<?>> visitedStructs;
	final Set<LegacyStruct> visitedLegacyStructs;
	final boolean backwardCompatible;
	final FunctionContext functionContext;

	LabelCollection(
			BitserCache cache, Set<String> declaredTargetLabels,
			boolean backwardCompatible, FunctionContext functionContext
	) {
		this.cache = cache;
		this.declaredTargets = declaredTargetLabels;
		this.stable = new HashSet<>();
		this.unstable = new HashSet<>();
		this.visitedStructs = new HashSet<>();
		this.visitedLegacyStructs = new HashSet<>();
		this.backwardCompatible = backwardCompatible;
		this.functionContext = functionContext;
	}
}
