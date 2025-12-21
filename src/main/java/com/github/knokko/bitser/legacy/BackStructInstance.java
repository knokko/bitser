package com.github.knokko.bitser.legacy;

public class BackStructInstance {

	public final int allowedClassIndex;
	public final BackClassInstance[] hierarchy;

	public BackStructInstance(int allowedClassIndex, BackClassInstance[] hierarchy) {
		this.allowedClassIndex = allowedClassIndex;
		this.hierarchy = hierarchy;
	}
}
