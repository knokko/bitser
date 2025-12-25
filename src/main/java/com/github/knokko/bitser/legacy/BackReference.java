package com.github.knokko.bitser.legacy;

public class BackReference {

	public final Object reference;

	public BackReference(Object reference) {
		this.reference = reference;
	}

	@Override
	public String toString() {
		return "reference to " + reference;
	}
}
