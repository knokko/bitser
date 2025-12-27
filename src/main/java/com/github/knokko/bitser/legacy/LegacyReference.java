package com.github.knokko.bitser.legacy;

public record LegacyReference(Object reference) {

	@Override
	public String toString() {
		return "reference to " + reference;
	}
}
