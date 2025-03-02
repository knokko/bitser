package com.github.knokko.bitser.backward.instance;

import java.util.List;
import java.util.UUID;

public class LegacyStructInstance {

	public final int inheritanceIndex;
	public final List<LegacyValues> valuesHierarchy;
	public final UUID stableID;
	public Object newInstance;

	public LegacyStructInstance(
			int inheritanceIndex, List<LegacyValues> valuesHierarchy, UUID stableID
	) {
		this.inheritanceIndex = inheritanceIndex;
		this.valuesHierarchy = valuesHierarchy;
		this.stableID = stableID;
	}
}
