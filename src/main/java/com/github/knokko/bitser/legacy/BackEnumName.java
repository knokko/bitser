package com.github.knokko.bitser.legacy;

public class BackEnumName {

	public final String name;

	public BackEnumName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "enum constant " + name;
	}
}
