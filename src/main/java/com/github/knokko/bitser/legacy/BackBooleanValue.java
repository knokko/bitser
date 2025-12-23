package com.github.knokko.bitser.legacy;

public class BackBooleanValue {

	public static final BackBooleanValue TRUE = new BackBooleanValue(true);
	public static final BackBooleanValue FALSE = new BackBooleanValue(false);

	public static BackBooleanValue get(boolean value) {
		return value ? TRUE : FALSE;
	}

	public final boolean value;

	private BackBooleanValue(boolean value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "boolean " + value;
	}
}
