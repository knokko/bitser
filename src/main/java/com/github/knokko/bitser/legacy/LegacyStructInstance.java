package com.github.knokko.bitser.legacy;

import com.github.knokko.bitser.util.JobOutput;

import java.util.List;
import java.util.UUID;

public class LegacyStructInstance {

	public final int inheritanceIndex;
	public final List<LegacyValues> valuesHierarchy;
	public final JobOutput<UUID> stableID;
	public Object newInstance;

	public LegacyStructInstance(
			int inheritanceIndex, List<LegacyValues> valuesHierarchy, JobOutput<UUID> stableID
	) {
		this.inheritanceIndex = inheritanceIndex;
		this.valuesHierarchy = valuesHierarchy;
		this.stableID = stableID;
	}
}
