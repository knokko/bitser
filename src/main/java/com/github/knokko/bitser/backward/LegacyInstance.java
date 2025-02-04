package com.github.knokko.bitser.backward;

import java.util.List;

public class LegacyInstance {

	public final int inheritanceIndex;
	public final List<LegacyValues> valuesHierarchy;

	public LegacyInstance(int inheritanceIndex, List<LegacyValues> valuesHierarchy) {
		this.inheritanceIndex = inheritanceIndex;
		this.valuesHierarchy = valuesHierarchy;
	}
}
