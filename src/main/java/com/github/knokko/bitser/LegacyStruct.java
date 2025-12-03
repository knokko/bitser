package com.github.knokko.bitser;

import com.github.knokko.bitser.legacy.LegacyStructInstance;
import com.github.knokko.bitser.legacy.LegacyValues;
import com.github.knokko.bitser.field.ReferenceField;
import com.github.knokko.bitser.util.JobOutput;
import com.github.knokko.bitser.util.Recursor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@BitStruct(backwardCompatible = false)
class LegacyStruct {

	@ReferenceField(stable = false, label = "classes")
	final ArrayList<LegacyClass> classHierarchy = new ArrayList<>();

	void collectReferenceLabels(LabelCollection labels) {
		if (!labels.visitedLegacyStructs.add(this)) return;
		for (LegacyClass legacyClass : classHierarchy) {
			legacyClass.collectReferenceLabels(labels);
		}
	}

	void read(Recursor<ReadContext, ReadInfo> recursor, int inheritanceIndex, Consumer setValue) {
		List<LegacyValues> artificial = new ArrayList<>(classHierarchy.size());
		JobOutput<UUID> stableID = null;
		for (LegacyClass legacyClass : classHierarchy) {
			LegacyValues classValues = legacyClass.read(recursor);
			artificial.add(classValues);
			if (classValues.stableID != null) stableID = classValues.stableID;
		}
		setValue.consume(new LegacyStructInstance(inheritanceIndex, artificial, stableID));
	}

	@FunctionalInterface
	interface Consumer {

		void consume(LegacyStructInstance instance);
	}
}
