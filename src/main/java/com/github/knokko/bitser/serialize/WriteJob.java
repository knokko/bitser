package com.github.knokko.bitser.serialize;

import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.util.ReferenceIdMapper;

public class WriteJob {

	public final BitOutputStream output;
	public final BitserCache cache;
	public final ReferenceIdMapper idMapper;
	public final boolean backwardCompatible;

	public WriteJob(BitOutputStream output, BitserCache cache, ReferenceIdMapper idMapper, boolean backwardCompatible) {
		this.output = output;
		this.cache = cache;
		this.idMapper = idMapper;
		this.backwardCompatible = backwardCompatible;
	}
}
