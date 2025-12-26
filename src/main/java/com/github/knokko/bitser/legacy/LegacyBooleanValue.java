package com.github.knokko.bitser.legacy;

public class LegacyBooleanValue {

	public static final LegacyBooleanValue TRUE = new LegacyBooleanValue(true);
	public static final LegacyBooleanValue FALSE = new LegacyBooleanValue(false);

	public static LegacyBooleanValue get(boolean value) {
		return value ? TRUE : FALSE;
	}

	public final boolean value;

	private LegacyBooleanValue(boolean value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "boolean " + value;
	}
}
