package com.github.knokko.bitser;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

class WriteInfo {

	final Bitser bitser;
	final Map<String, Object> withParameters;
	final LegacyClasses legacy;

	WriteInfo(Bitser bitser, Map<String, Object> withParameters, LegacyClasses legacy) {
		this.bitser = Objects.requireNonNull(bitser);
		this.withParameters = Collections.unmodifiableMap(withParameters);
		this.legacy = legacy;
	}
}
