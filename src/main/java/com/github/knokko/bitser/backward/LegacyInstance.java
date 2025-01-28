package com.github.knokko.bitser.backward;

import java.util.List;
import java.util.UUID;

public class LegacyInstance {

	public final int inheritanceIndex;
	public final List<LegacyValues> valuesHierarchy;
	public final UUID stableID;
	public Object recoveredInstance;

	public LegacyInstance(int inheritanceIndex, List<LegacyValues> valuesHierarchy, UUID stableID) {
		this.inheritanceIndex = inheritanceIndex;
		this.valuesHierarchy = valuesHierarchy;
		this.stableID = stableID;
	}
}
