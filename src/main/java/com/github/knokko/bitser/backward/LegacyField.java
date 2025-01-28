package com.github.knokko.bitser.backward;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.field.ClassField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.wrapper.BitFieldWrapper;

@BitStruct(backwardCompatible = false)
public class LegacyField {

	@IntegerField(expectUniform = false, minValue = 0)
	public final int id;

	@ClassField(root = BitFieldWrapper.class)
	public final BitFieldWrapper bitField;

	public LegacyField(int id, BitFieldWrapper bitField) {
		this.id = id;
		this.bitField = bitField;
	}

	@SuppressWarnings("unused")
	private LegacyField() {
		this(0, null);
	}
}
