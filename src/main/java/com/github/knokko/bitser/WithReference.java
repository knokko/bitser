package com.github.knokko.bitser;

public class WithReference {

	public final Object reference;

	public WithReference(Object reference) {
		this.reference = reference;
	}

	@Override
	public String toString() {
		return "with reference to " + reference;
	}
}
