package com.github.knokko.bitser;

import com.github.knokko.bitser.field.ClassField;
import com.github.knokko.bitser.field.IntegerField;

@BitStruct(backwardCompatible = false)
class LegacyField {

	@IntegerField(expectUniform = false, minValue = 0)
	final int id;

	@ClassField(root = BitFieldWrapper.class)
	final BitFieldWrapper bitField;

	LegacyField(int id, BitFieldWrapper bitField) {
		this.id = id;
		this.bitField = bitField;
	}

	@SuppressWarnings("unused")
	private LegacyField() {
		this(0, null);
	}
}
