package com.github.knokko.bitser.legacy;

public class LegacyClassValues {

	public final Object[] fieldValues;
	public final boolean[] hasFieldValues;

	public final Object[] functionValues;
	public final boolean[] hasFunctionValues;

	public LegacyClassValues(int largestFieldID, int largestFunctionID) {
		this.fieldValues = new Object[largestFieldID + 1];
		this.hasFieldValues = new boolean[largestFieldID + 1];
		this.functionValues = new Object[largestFunctionID + 1];
		this.hasFunctionValues = new boolean[largestFunctionID + 1];
	}
}
