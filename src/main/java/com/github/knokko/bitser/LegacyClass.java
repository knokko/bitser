package com.github.knokko.bitser;

import com.github.knokko.bitser.legacy.LegacyClassValues;
import com.github.knokko.bitser.field.BitField;

import java.util.ArrayList;

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

	LegacyClassValues constructEmptyInstance() {
		return new LegacyClassValues(largestFieldId, largestFunctionId);
	}
}
