package com.github.knokko.bitser.serialize;

import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.util.ReferenceIdLoader;

import java.util.Collections;
import java.util.Map;

public class ReadJob {

	public final Bitser bitser;
	public final BitInputStream input;
	public final ReferenceIdLoader idLoader;
	public final Map<String, Object> withParameters;
	public final boolean backwardCompatible;

	public ReadJob(
			Bitser bitser, BitInputStream input,
			ReferenceIdLoader idLoader, Map<String, Object> withParameters,
			boolean backwardCompatible
	) {
		this.bitser = bitser;
		this.input = input;
		this.idLoader = idLoader;
		this.withParameters = Collections.unmodifiableMap(withParameters);
		this.backwardCompatible = backwardCompatible;
	}
}
