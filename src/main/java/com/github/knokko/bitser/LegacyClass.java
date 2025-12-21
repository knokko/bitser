package com.github.knokko.bitser;

import com.github.knokko.bitser.legacy.BackClassInstance;
import com.github.knokko.bitser.legacy.BackStructInstance;
import com.github.knokko.bitser.legacy.LegacyValues;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.util.JobOutput;
import com.github.knokko.bitser.util.Recursor;

import java.util.ArrayList;
import java.util.UUID;

import static java.lang.Math.max;

@BitStruct(backwardCompatible = false)
class LegacyClass implements BitPostInit {

	@BitField
	final ArrayList<LegacyField> fields = new ArrayList<>();

	@BitField
	final ArrayList<LegacyField> functions = new ArrayList<>();

	private int largestFieldId = -1, largestFunctionId = -1;

	@Override
	public void postInit(BitPostInit.Context context) {
		for (LegacyField field : fields) largestFieldId = max(largestFieldId, field.id);
		for (LegacyField function : functions) largestFunctionId = max(largestFunctionId, function.id);
	}

	@Override
	public String toString() {
		return "LegacyClass(#fields=" + fields.size() + ",#functions=" + functions.size() + ")";
	}

	void collectReferenceLabels(Recursor<LabelContext, LabelInfo> recursor) {
		for (LegacyField field : fields) {
			field.bitField.collectReferenceLabels(recursor);
		}
		for (LegacyField field : functions) {
			field.bitField.collectReferenceLabels(recursor);
		}
	}

	BackClassInstance constructEmptyInstance() {
		return new BackClassInstance(largestFieldId, largestFunctionId);
	}

	LegacyValues read(Recursor<ReadContext, ReadInfo> recursor) {
		int maxId = -1;
		for (LegacyField field : fields) maxId = max(maxId, field.id);
		Object[] artificial = new Object[maxId + 1];
		boolean[] hadValues = new boolean[maxId + 1];
		boolean[] hadReferenceValues = new boolean[maxId + 1];
		JobOutput<UUID> stableID = null;

		for (LegacyField field : fields) {
			hadValues[field.id] = true;
			if (field.bitField.isReference()) hadReferenceValues[field.id] = true;
			recursor.runNested("field " + field.id, nested ->
					field.bitField.readField(nested, child -> artificial[field.id] = child)
			);
			if (field.bitField instanceof UUIDFieldWrapper && ((UUIDFieldWrapper) field.bitField).isStableReferenceId) {
				stableID = recursor.computeFlat("stable-id", context -> (UUID) artificial[field.id]);
			}
		}

		maxId = -1;
		for (LegacyField function : functions) maxId = max(maxId, function.id);
		Object[] functionValues = new Object[maxId + 1];
		boolean[] hadFunctionValues = new boolean[maxId + 1];
		boolean[] hadReferenceFunctions = new boolean[maxId + 1];

		for (LegacyField function : functions) {
			hadFunctionValues[function.id] = true;
			if (function.bitField.isReference()) hadReferenceFunctions[function.id] = true;
			recursor.runNested("function " + function.id, nested ->
					function.bitField.readField(nested, value -> functionValues[function.id] = value)
			);
		}

		return new LegacyValues(
				artificial, hadValues, hadReferenceValues,
				functionValues, hadFunctionValues, hadReferenceFunctions, stableID
		);
	}
}
