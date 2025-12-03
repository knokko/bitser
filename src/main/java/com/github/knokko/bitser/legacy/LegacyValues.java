package com.github.knokko.bitser.legacy;

import com.github.knokko.bitser.util.JobOutput;

import java.util.UUID;

public class LegacyValues {

	public final Object[] values;
	public final boolean[] hadValues;
	public final boolean[] hadReferenceValues;
	public final Object[] storedFunctionValues;
	public Object[] convertedFunctionValues;
	public final boolean[] hadFunctionValues;
	public final boolean[] hadReferenceFunctions;
	public final JobOutput<UUID> stableID;

	public LegacyValues(
			Object[] values, boolean[] hadValues, boolean[] hadReferenceValues,
			Object[] storedFunctionValues, boolean[] hadFunctionValues,
			boolean[] hadReferenceFunctions, JobOutput<UUID> stableID
	) {
		this.values = values;
		this.hadValues = hadValues;
		this.hadReferenceValues = hadReferenceValues;
		this.storedFunctionValues = storedFunctionValues;
		this.hadFunctionValues = hadFunctionValues;
		this.hadReferenceFunctions = hadReferenceFunctions;
		this.stableID = stableID;
	}
}
