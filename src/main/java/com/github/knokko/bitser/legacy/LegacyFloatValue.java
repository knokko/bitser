package com.github.knokko.bitser.legacy;

public record LegacyFloatValue(double value) {

	@Override
	public String toString() {
		return "floating " + value;
	}
}
