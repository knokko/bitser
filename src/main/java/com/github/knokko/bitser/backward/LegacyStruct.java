package com.github.knokko.bitser.backward;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.field.ReferenceField;
import com.github.knokko.bitser.serialize.LabelCollection;
import com.github.knokko.bitser.serialize.ReadJob;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@BitStruct(backwardCompatible = false)
public class LegacyStruct {

	@ReferenceField(stable = false, label = "classes")
	public final ArrayList<LegacyClass> classHierarchy = new ArrayList<>();

	public void collectReferenceTargetLabels(LabelCollection labels) {
		for (LegacyClass legacyClass : classHierarchy) {
			legacyClass.collectReferenceTargetLabels(labels);
		}
	}

	public void read(ReadJob read, Consumer setValue) throws IOException {
		List<LegacyValues> artificial = new ArrayList<>(classHierarchy.size());
		for (LegacyClass legacyClass : classHierarchy) {
			artificial.add(legacyClass.read(read));
		}
		setValue.consume(artificial);
	}

	@FunctionalInterface
	public interface Consumer {

		void consume(List<LegacyValues> values) throws IOException;
	}
}
