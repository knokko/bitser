package com.github.knokko.bitser.backward;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.field.ReferenceField;
import com.github.knokko.bitser.field.ReferenceFieldTarget;
import com.github.knokko.bitser.serialize.LabelCollection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// Ironically, I don't think I can make this backward-compatible
@BitStruct(backwardCompatible = false)
public class LegacyClasses {

	@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
	@ReferenceFieldTarget(label = "classes")
	private final ArrayList<LegacyClass> classes = new ArrayList<>();

	@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
	@ReferenceFieldTarget(label = "structs")
	private final ArrayList<LegacyStruct> structs = new ArrayList<>();

	private final Map<Class<?>, LegacyClass> classMap = new HashMap<>();
	private final Map<Class<?>, LegacyStruct> structMap = new HashMap<>();

	@ReferenceField(stable = false, label = "structs")
	private LegacyStruct root;

	public void setRoot(LegacyStruct root) {
		if (this.root != null) throw new IllegalStateException();
		this.root = root;
	}

	public void collectReferenceLabels(LabelCollection labels) {
		for (LegacyClass legacyClass : classes) legacyClass.collectReferenceLabels(labels);
	}

	public LegacyStruct getRoot() {
		return root;
	}

	public LegacyClass addClass(Class<?> javaClass) {
		if (!classMap.containsKey(javaClass)) {
			LegacyClass legacyClass = new LegacyClass();
			classMap.put(javaClass, legacyClass);
			classes.add(legacyClass);
		}

		return classMap.get(javaClass);
	}

	public LegacyStruct addStruct(Class<?> javaClass) {
		if (!structMap.containsKey(javaClass)) {
			LegacyStruct legacyStruct = new LegacyStruct();
			structMap.put(javaClass, legacyStruct);
			structs.add(legacyStruct);
		}

		return structMap.get(javaClass);
	}

	public LegacyStruct getStruct(Class<?> javaClass) {
		return structMap.get(javaClass);
	}
}
