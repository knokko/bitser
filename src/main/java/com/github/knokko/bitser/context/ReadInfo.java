package com.github.knokko.bitser.context;

import com.github.knokko.bitser.serialize.Bitser;
import com.github.knokko.bitser.serialize.CollectionSizeLimit;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class ReadInfo {

	public final Bitser bitser;
	public final Map<String, Object> withParameters;
	public final boolean backwardCompatible;
	public final CollectionSizeLimit sizeLimit;

	public ReadInfo(
			Bitser bitser, Map<String, Object> withParameters,
			boolean backwardCompatible, CollectionSizeLimit sizeLimit
	) {
		this.bitser = Objects.requireNonNull(bitser);
		this.withParameters = Collections.unmodifiableMap(withParameters);
		this.backwardCompatible = backwardCompatible;
		this.sizeLimit = sizeLimit;
	}
}
