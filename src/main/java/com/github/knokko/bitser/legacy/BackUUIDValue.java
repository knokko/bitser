package com.github.knokko.bitser.legacy;

import java.util.UUID;

public class BackUUIDValue {

	public final UUID value;

	public BackUUIDValue(UUID value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "id " + value;
	}
}
