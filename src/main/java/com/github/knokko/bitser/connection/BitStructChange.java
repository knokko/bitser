package com.github.knokko.bitser.connection;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.field.NestedFieldSetting;

@BitStruct(backwardCompatible = false)
public class BitStructChange {

	@BitField(ordering = 0)
	@IntegerField(expectUniform = false, minValue = 0)
	public final int fieldOrdering;

	@BitField(ordering = 1)
	public final boolean isNested;

	@BitField(ordering = 2)
	@NestedFieldSetting(path = "", writeAsBytes = true)
	public final byte[] payload;

	public BitStructChange(int fieldOrdering, boolean isNested, byte[] payload) {
		this.fieldOrdering = fieldOrdering;
		this.isNested = isNested;
		this.payload = payload;
	}

	@SuppressWarnings("unused")
	private BitStructChange() {
		this(-1, false, null);
	}

	@Override
	public String toString() {
		return "Change(field=" + fieldOrdering + ", nested=" + isNested + ", size=" + payload.length + ")";
	}
}
