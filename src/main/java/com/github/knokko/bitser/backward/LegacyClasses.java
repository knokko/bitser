package com.github.knokko.bitser.backward;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.field.ReferenceFieldTarget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// Ironically, I don't think I can make this backward-compatible
@BitStruct(backwardCompatible = false)
public class LegacyClasses {

	@ReferenceFieldTarget(label = "classes")
	private final ArrayList<LegacyClass> classes = new ArrayList<>();

	@ReferenceFieldTarget(label = "structs")
	private final ArrayList<LegacyStruct> structs = new ArrayList<>();

	private final Map<Class<?>, LegacyClass> classMap = new HashMap<>();
	private final Map<Class<?>, LegacyStruct> structMap = new HashMap<>();

	private LegacyStruct root;

	public void setRoot(LegacyStruct root) {
		if (this.root != null) throw new IllegalStateException();
		this.root = root;
	}

	public void add(Class<?> javaClass, LegacyClass legacyClass) {
		if (!classMap.containsKey(javaClass)) {
			classMap.put(javaClass, legacyClass);
			classes.add(legacyClass);
		}
	}

	public void add(Class<?> javaClass, LegacyStruct legacyStruct) {
		if (!structMap.containsKey(javaClass)) {
			structMap.put(javaClass, legacyStruct);
			structs.add(legacyStruct);
		}
	}

	public LegacyStruct getStruct(Class<?> javaClass) {
		return structMap.get(javaClass);
	}
}
