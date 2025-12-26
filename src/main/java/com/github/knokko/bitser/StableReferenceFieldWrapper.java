package com.github.knokko.bitser;

import com.github.knokko.bitser.util.Recursor;

import java.util.function.Consumer;

@BitStruct(backwardCompatible = false)
class StableReferenceFieldWrapper extends ReferenceFieldWrapper {

	StableReferenceFieldWrapper(VirtualField field, String label) {
		super(field, label);
	}

	@SuppressWarnings("unused")
	private StableReferenceFieldWrapper() {
		super();
	}
	// TODO Delete this class
}
