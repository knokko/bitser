package com.github.knokko.bitser.legacy;

import java.util.UUID;

public class LegacyStructInstance {

	public final int allowedClassIndex;
	public final LegacyClassValues[] hierarchy;

	public UUID stableID;

	public LegacyStructInstance(int allowedClassIndex, LegacyClassValues[] hierarchy) {
		this.allowedClassIndex = allowedClassIndex;
		this.hierarchy = hierarchy;
	}

	@Override
	public String toString() {
		return "BackStructInstance";
	}
}
