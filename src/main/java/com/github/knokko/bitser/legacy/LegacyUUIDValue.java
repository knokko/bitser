package com.github.knokko.bitser.legacy;

import java.util.UUID;

public class LegacyUUIDValue {

	public final UUID value;

	public LegacyUUIDValue(UUID value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "id " + value;
	}
}
