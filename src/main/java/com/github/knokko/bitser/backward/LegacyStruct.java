package com.github.knokko.bitser.backward;

import com.github.knokko.bitser.BitStruct;
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

	public void collectReferenceTargetLabels(LabelCollection labels) {
		for (LegacyClass legacyClass : classHierarchy) {
			legacyClass.collectReferenceTargetLabels(labels);
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
		setValue.consume(new LegacyInstance(inheritanceIndex, artificial, stableID));
	}

	@FunctionalInterface
	public interface Consumer {

		void consume(LegacyInstance instance) throws IOException;
	}
}
