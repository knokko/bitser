package com.github.knokko.bitser.backward;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.backward.instance.LegacyStructInstance;
import com.github.knokko.bitser.backward.instance.LegacyValues;
import com.github.knokko.bitser.field.ReferenceField;
import com.github.knokko.bitser.serialize.LabelCollection;
import com.github.knokko.bitser.serialize.ReadJob;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@BitStruct(backwardCompatible = false)
public class LegacyStruct {

	@ReferenceField(stable = false, label = "classes")
	public final ArrayList<LegacyClass> classHierarchy = new ArrayList<>();

	public void collectReferenceLabels(LabelCollection labels) {
		if (!labels.visitedLegacyStructs.add(this)) return;
		for (LegacyClass legacyClass : classHierarchy) {
			legacyClass.collectReferenceLabels(labels);
		}
	}

	public void read(ReadJob read, int inheritanceIndex, Consumer setValue) throws IOException {
		List<LegacyValues> artificial = new ArrayList<>(classHierarchy.size());
		UUID stableID = null;
		for (LegacyClass legacyClass : classHierarchy) {
			LegacyValues classValues = legacyClass.read(read);
			artificial.add(classValues);
			if (classValues.stableID != null) stableID = classValues.stableID;
		}
		setValue.consume(new LegacyStructInstance(inheritanceIndex, artificial, stableID));
	}

	@FunctionalInterface
	public interface Consumer {

		void consume(LegacyStructInstance instance) throws IOException;
	}
}
