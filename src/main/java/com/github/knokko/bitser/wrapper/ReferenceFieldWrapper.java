package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.serialize.BitserCache;
import com.github.knokko.bitser.util.VirtualField;

abstract class ReferenceFieldWrapper extends BitFieldWrapper {

	@BitField
	final String label;

	ReferenceFieldWrapper(VirtualField field, String label) {
		super(field);
		this.label = label;
	}

	ReferenceFieldWrapper() {
		super();
		this.label = "";
	}

	@Override
	public boolean isReference() {
		return true;
	}

	@Override
	boolean deepEquals(Object a, Object b, BitserCache cache) {
		return a == b;
	}

	@Override
	int hashCode(Object value, BitserCache cache) {
		// I don't want the hash code to change when the referenced value changes
		return 12345;
	}
}
