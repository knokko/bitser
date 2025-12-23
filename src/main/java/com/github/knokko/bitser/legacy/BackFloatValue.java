package com.github.knokko.bitser.legacy;

public class BackFloatValue {

	public final double value;

	public BackFloatValue(double value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "floating " + value;
	}
}
