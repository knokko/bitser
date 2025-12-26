package com.github.knokko.bitser;

@BitStruct(backwardCompatible = false)
class UnstableReferenceFieldWrapper extends ReferenceFieldWrapper {

	// TODO Delete this class?
	UnstableReferenceFieldWrapper(VirtualField field, String label) {
		super(field, label);
	}

	@SuppressWarnings("unused")
	private UnstableReferenceFieldWrapper() {
		super();
	}
}
