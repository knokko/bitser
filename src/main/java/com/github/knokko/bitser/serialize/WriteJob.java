package com.github.knokko.bitser.serialize;

import com.github.knokko.bitser.backward.LegacyClasses;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.util.ReferenceIdMapper;

import java.util.Collections;
import java.util.Map;

public class WriteJob {

	public final BitOutputStream output;
	public final BitserCache cache;
	public final ReferenceIdMapper idMapper;
	public final Map<String, Object> withParameters;
	public final LegacyClasses legacy;

	public WriteJob(
			BitOutputStream output, BitserCache cache,
			ReferenceIdMapper idMapper, Map<String, Object> withParameters,
			LegacyClasses legacy
	) {
		this.output = output;
		this.cache = cache;
		this.idMapper = idMapper;
		this.withParameters = Collections.unmodifiableMap(withParameters);
		this.legacy = legacy;
	}
}
