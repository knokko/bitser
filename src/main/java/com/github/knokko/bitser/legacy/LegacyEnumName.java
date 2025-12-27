package com.github.knokko.bitser.legacy;

public record LegacyEnumName(String name) {

	@Override
	public String toString() {
		return "enum constant " + name;
	}
}
