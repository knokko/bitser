package com.github.knokko.bitser.serialize;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.field.ClassField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.wrapper.BitFieldWrapper;

@BitStruct(backwardCompatible = false)
public class SavedField {

	@IntegerField(expectUniform = false, minValue = 0)
	public final int id;

	@ClassField(root = BitFieldWrapper.class)
	public final BitFieldWrapper bitField;

	public SavedField(int id, BitFieldWrapper bitField) {
		this.id = id;
		this.bitField = bitField;
	}
}
