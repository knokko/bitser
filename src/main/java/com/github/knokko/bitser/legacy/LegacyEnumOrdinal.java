package com.github.knokko.bitser.legacy;

public class LegacyEnumOrdinal {

	public final int ordinal;

	public LegacyEnumOrdinal(int ordinal) {
		this.ordinal = ordinal;
	}

	@Override
	public String toString() {
		return "enum ordinal " + ordinal;
	}
}
