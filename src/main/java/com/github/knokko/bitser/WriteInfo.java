package com.github.knokko.bitser;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

class WriteInfo {

	final Bitser bitser;
	final Map<String, Object> withParameters;
	final LegacyClasses legacy;
	final boolean usesContextInfo;
	final boolean forbidLazySaving;

	WriteInfo(
			Bitser bitser, Map<String, Object> withParameters, LegacyClasses legacy,
			boolean usesContextInfo, boolean forbidLazySaving
	) {
		this.bitser = Objects.requireNonNull(bitser);
		this.withParameters = Collections.unmodifiableMap(withParameters);
		this.legacy = legacy;
		this.usesContextInfo = usesContextInfo;
		this.forbidLazySaving = forbidLazySaving;
	}
}
