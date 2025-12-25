package com.github.knokko.bitser.legacy;

public class BackStringValue {

	public final String value;

	public BackStringValue(String value) {
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
		return other instanceof BackStringValue && this.value.equals(((BackStringValue) other).value);
	}
}
