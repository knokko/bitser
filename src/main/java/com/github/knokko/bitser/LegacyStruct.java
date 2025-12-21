package com.github.knokko.bitser;

import com.github.knokko.bitser.legacy.BackClassInstance;
import com.github.knokko.bitser.legacy.BackStructInstance;
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

	BackStructInstance constructEmptyInstance(int allowedClassIndex) {
		BackClassInstance[] classes = new BackClassInstance[classHierarchy.size()];
		for (int index = 0; index < classHierarchy.size(); index++) {
			classes[index] = classHierarchy.get(index).constructEmptyInstance();
		}
		return new BackStructInstance(allowedClassIndex, classes);
	}

	void collectReferenceLabels(Recursor<LabelContext, LabelInfo> recursor) {
		JobOutput<Boolean> alreadyContainedThisStruct = recursor.computeFlat(
				"already-container", context -> !context.visitedLegacyStructs.add(this)
		);
		for (LegacyClass legacyClass : classHierarchy) {
			recursor.runNested("legacy-class", nested -> {
				if (!alreadyContainedThisStruct.get()) {
					legacyClass.collectReferenceLabels(nested);
				}
			});
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
