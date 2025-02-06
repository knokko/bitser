package com.github.knokko.bitser.backward;

public class LegacyValues {

	public final Object[] values;
	public final boolean[] hadValues;
	public final Object[] storedFunctionValues;
	public Object[] convertedFunctionValues;
	public final boolean[] hadFunctionValues;

	public LegacyValues(
			Object[] values, boolean[] hadValues, Object[] storedFunctionValues, boolean[] hadFunctionValues
	) {
		this.values = values;
		this.hadValues = hadValues;
		this.storedFunctionValues = storedFunctionValues;
		this.hadFunctionValues = hadFunctionValues;
	}
}
