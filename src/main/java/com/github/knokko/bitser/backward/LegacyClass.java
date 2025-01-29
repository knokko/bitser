package com.github.knokko.bitser.backward;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.field.BitField;

import java.util.ArrayList;

// Ironically, I don't think I can make this backward-compatible
@BitStruct(backwardCompatible = false)
public class LegacyClass {

	@BitField
	public final ArrayList<LegacyField> fields = new ArrayList<>();

	@Override
	public String toString() {
		return "LegacyClass(#fields=" + fields.size() + ")";
	}
}
