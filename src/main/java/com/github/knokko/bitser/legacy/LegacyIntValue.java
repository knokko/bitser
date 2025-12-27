package com.github.knokko.bitser.legacy;

public record LegacyIntValue(long value) {

	@Override
	public String toString() {
		return "integer " + value;
	}
}
