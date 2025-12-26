package com.github.knokko.bitser.legacy;

public class LegacyFloatValue {

	public final double value;

	public LegacyFloatValue(double value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "floating " + value;
	}
}
