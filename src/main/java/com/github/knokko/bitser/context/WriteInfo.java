package com.github.knokko.bitser.context;

import com.github.knokko.bitser.backward.LegacyClasses;
import com.github.knokko.bitser.serialize.Bitser;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class WriteInfo {

	public final Bitser bitser;
	public final Map<String, Object> withParameters;
	public final LegacyClasses legacy;

	public WriteInfo(Bitser bitser, Map<String, Object> withParameters, LegacyClasses legacy) {
		this.bitser = Objects.requireNonNull(bitser);
		this.withParameters = Collections.unmodifiableMap(withParameters);
		this.legacy = legacy;
	}
}
