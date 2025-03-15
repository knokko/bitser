package com.github.knokko.bitser.serialize;

import com.github.knokko.bitser.backward.LegacyClasses;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.util.ReferenceIdMapper;

import java.util.Collections;
import java.util.Map;

public class WriteJob {

	public final Bitser bitser;
	public final BitOutputStream output;
	public final ReferenceIdMapper idMapper;
	public final Map<String, Object> withParameters;
	public final LegacyClasses legacy;

	public WriteJob(
			Bitser bitser, BitOutputStream output,
			ReferenceIdMapper idMapper, Map<String, Object> withParameters,
			LegacyClasses legacy
	) {
		this.bitser = bitser;
		this.output = output;
		this.idMapper = idMapper;
		this.withParameters = Collections.unmodifiableMap(withParameters);
		this.legacy = legacy;
	}
}
