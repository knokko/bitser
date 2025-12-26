package com.github.knokko.bitser.legacy;

public class LegacyStringValue {

	public final String value;

	public LegacyStringValue(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "string " + value;
	}

	@Override
	public int hashCode() {
		return 1 + value.hashCode();
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof LegacyStringValue && this.value.equals(((LegacyStringValue) other).value);
	}
}
