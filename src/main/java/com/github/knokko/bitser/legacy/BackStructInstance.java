package com.github.knokko.bitser.legacy;

import java.util.UUID;

public class BackStructInstance {

	public final int allowedClassIndex;
	public final BackClassInstance[] hierarchy;

	public UUID stableID;

	public BackStructInstance(int allowedClassIndex, BackClassInstance[] hierarchy) {
		this.allowedClassIndex = allowedClassIndex;
		this.hierarchy = hierarchy;
	}

	@Override
	public String toString() {
		return "BackStructInstance";
	}
}
