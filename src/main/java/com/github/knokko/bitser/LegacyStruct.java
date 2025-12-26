package com.github.knokko.bitser;

import com.github.knokko.bitser.legacy.BackClassInstance;
import com.github.knokko.bitser.legacy.BackStructInstance;
import com.github.knokko.bitser.field.ReferenceField;

import java.util.ArrayList;

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
}
