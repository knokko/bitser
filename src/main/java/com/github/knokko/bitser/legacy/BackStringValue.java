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
}
