package com.github.knokko.bitser.serialize;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.field.BitField;

import java.util.List;

// Ironically, I don't think I can make this backward-compatible
@BitStruct(backwardCompatible = false)
public class SavedClass {

	public final Class<?> javaClass;

	@BitField
	public final String id;

	@BitField
	public final List<SavedField> fields;

	public SavedClass(Class<?> javaClass, String id, List<SavedField> fields) {
		this.javaClass = javaClass;
		this.id = id;
		this.fields = fields;
	}
}
