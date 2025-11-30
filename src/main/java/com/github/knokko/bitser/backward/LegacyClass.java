package com.github.knokko.bitser.backward;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.backward.instance.LegacyValues;
import com.github.knokko.bitser.context.ReadContext;
import com.github.knokko.bitser.context.ReadInfo;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.serialize.LabelCollection;
import com.github.knokko.bitser.util.JobOutput;
import com.github.knokko.bitser.util.Recursor;
import com.github.knokko.bitser.wrapper.UUIDFieldWrapper;

import java.util.ArrayList;
import java.util.UUID;

import static java.lang.Math.max;

@BitStruct(backwardCompatible = false)
public class LegacyClass {

	@BitField
	public final ArrayList<LegacyField> fields = new ArrayList<>();

	@BitField
	public final ArrayList<LegacyField> functions = new ArrayList<>();

	@Override
	public String toString() {
		return "LegacyClass(#fields=" + fields.size() + ",#functions=" + functions.size() + ")";
	}

	public void collectReferenceLabels(LabelCollection labels) {
		for (LegacyField field : fields) {
			field.bitField.collectReferenceLabels(labels);
		}
		for (LegacyField field : functions) {
			field.bitField.collectReferenceLabels(labels);
		}
	}

	public LegacyValues read(Recursor<ReadContext, ReadInfo> recursor) {
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
