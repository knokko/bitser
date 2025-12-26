package com.github.knokko.bitser.legacy;

public class LegacyEnumName {

	public final String name;

	public LegacyEnumName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "enum constant " + name;
	}
}
