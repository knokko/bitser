package com.github.knokko.bitser.legacy;

public record LegacyStringValue(String value) {

	@Override
	public String toString() {
		return "string " + value;
	}
}
