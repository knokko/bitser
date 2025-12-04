package com.github.knokko.bitser;

import java.util.HashSet;
import java.util.Set;

public class LabelContext {

	final Set<String> declaredTargets, stable, unstable;
	final Set<BitStructWrapper<?>> visitedStructs;
	final Set<LegacyStruct> visitedLegacyStructs;

	LabelContext(Set<String> declaredTargetLabels) {
		this.declaredTargets = declaredTargetLabels;
		this.stable = new HashSet<>();
		this.unstable = new HashSet<>();
		this.visitedStructs = new HashSet<>();
		this.visitedLegacyStructs = new HashSet<>();
	}
}
