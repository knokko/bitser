package com.github.knokko.bitser.legacy;

public class LegacyReference {

	public final Object reference;

	public LegacyReference(Object reference) {
		this.reference = reference;
	}

	@Override
	public String toString() {
		return "reference to " + reference;
	}
}
