package com.github.knokko.bitser.legacy;

public class LegacyIntValue {

	public final long value;

	public LegacyIntValue(long value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "integer " + value;
	}
}
