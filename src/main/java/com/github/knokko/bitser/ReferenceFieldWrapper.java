package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.UnexpectedBitserException;
import com.github.knokko.bitser.field.BitField;

@BitStruct(backwardCompatible = false)
class ReferenceFieldWrapper extends BitFieldWrapper {

	@BitField
	final String label;

	@BitField
	final boolean stable;

	ReferenceFieldWrapper(VirtualField field, String label, boolean stable) {
		super(field);
		this.label = label;
		this.stable = stable;
	}

	@SuppressWarnings("unused")
	private ReferenceFieldWrapper() {
		super();
		this.label = "";
		this.stable = false;
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

	@Override
	void write(
			Serializer serializer, Object value,
			RecursionNode parentNode, String fieldName
	) {
		throw new UnexpectedBitserException("Reference fields should get special treatment");
	}

	@Override
	Object read(Deserializer deserializer, RecursionNode parentNode, String fieldName) {
		throw new UnexpectedBitserException("Reference fields should get special treatment");
	}

	@Override
	Object read(BackDeserializer deserializer, RecursionNode parentNode, String fieldName) {
		throw new UnexpectedBitserException("Reference fields should get special treatment");
	}

	@Override
	Object convert(BackDeserializer deserializer, Object legacyValue, RecursionNode parentNode, String fieldName) {
		throw new UnexpectedBitserException("Reference fields should get special treatment");
	}
}
