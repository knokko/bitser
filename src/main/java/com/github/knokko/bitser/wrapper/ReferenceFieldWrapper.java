package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.backward.LegacyInstance;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.serialize.ReadJob;
import com.github.knokko.bitser.util.VirtualField;

import java.util.function.Consumer;

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
	void setLegacyValue(ReadJob read, Object value, Consumer<Object> setValue) {}

	@Override
	void setLegacyReference(ReadJob read, Object value, Consumer<Object> setValue) {
		if (value instanceof LegacyInstance) {
			super.setLegacyValue(read, ((LegacyInstance) value).recoveredInstance, setValue);
		} else {
			super.setLegacyValue(read, value, setValue);
		}
	}

	@Override
	boolean delayLegacyUntilResolve() {
		return true;
	}
}
