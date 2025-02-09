package com.github.knokko.bitser.backward;

import java.util.UUID;

public class LegacyValues {

	public final Object[] values;
	public final boolean[] hadValues;
	public final Object[] storedFunctionValues;
	public Object[] convertedFunctionValues;
	public final boolean[] hadFunctionValues;
	final UUID stableID;

	public LegacyValues(
			Object[] values, boolean[] hadValues, Object[] storedFunctionValues, boolean[] hadFunctionValues, UUID stableID
	) {
		this.values = values;
		this.hadValues = hadValues;
		this.storedFunctionValues = storedFunctionValues;
		this.hadFunctionValues = hadFunctionValues;
		this.stableID = stableID;
	}
}
