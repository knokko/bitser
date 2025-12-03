package com.github.knokko.bitser;

import com.github.knokko.bitser.options.CollectionSizeLimit;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

class ReadInfo {

	final Bitser bitser;
	final Map<String, Object> withParameters;
	final boolean backwardCompatible;
	final CollectionSizeLimit sizeLimit;

	ReadInfo(
			Bitser bitser, Map<String, Object> withParameters,
			boolean backwardCompatible, CollectionSizeLimit sizeLimit
	) {
		this.bitser = Objects.requireNonNull(bitser);
		this.withParameters = Collections.unmodifiableMap(withParameters);
		this.backwardCompatible = backwardCompatible;
		this.sizeLimit = sizeLimit;
	}
}
