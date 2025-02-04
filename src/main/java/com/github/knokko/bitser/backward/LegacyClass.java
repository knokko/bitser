package com.github.knokko.bitser.backward;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.serialize.LabelCollection;
import com.github.knokko.bitser.serialize.ReadJob;

import java.io.IOException;
import java.util.ArrayList;

@BitStruct(backwardCompatible = false)
public class LegacyClass {

	@BitField
	public final ArrayList<LegacyField> fields = new ArrayList<>();

	@Override
	public String toString() {
		return "LegacyClass(#fields=" + fields.size() + ")";
	}

	public void collectReferenceTargetLabels(LabelCollection labels) {
		for (LegacyField field : fields) {
			field.bitField.collectReferenceTargetLabels(labels);
		}
	}

	public LegacyValues read(ReadJob read) throws IOException {
		int maxId = -1;
		for (LegacyField field : fields) {
			if (field.id > maxId) maxId = field.id;
		}
		Object[] artificial = new Object[maxId + 1];
		boolean[] hadValues = new boolean[maxId + 1];

		for (LegacyField field : fields) {
			hadValues[field.id] = true;
			field.bitField.read(read, child -> artificial[field.id] = child);
			System.out.println("Legacy read " + artificial[field.id]);
		}

		return new LegacyValues(artificial, hadValues);
	}
}
