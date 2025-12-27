package com.github.knokko.bitser.legacy;

public record LegacyEnumOrdinal(int ordinal) {

	@Override
	public String toString() {
		return "enum ordinal " + ordinal;
	}
}
