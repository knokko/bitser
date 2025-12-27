package com.github.knokko.bitser.legacy;

import java.util.UUID;

public record LegacyUUIDValue(UUID value) {

	@Override
	public String toString() {
		return "id " + value;
	}
}
