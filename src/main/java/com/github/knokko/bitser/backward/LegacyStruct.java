package com.github.knokko.bitser.backward;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.field.ReferenceField;

import java.util.ArrayList;

@BitStruct(backwardCompatible = false)
public class LegacyStruct {

	@ReferenceField(stable = false, label = "classes")
	public final ArrayList<LegacyClass> classHierarchy = new ArrayList<>();
}
