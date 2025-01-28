package com.github.knokko.bitser.serialize;

import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.util.ReferenceIdLoader;

public class ReadJob {

	public final BitInputStream input;
	public final BitserCache cache;
	public final ReferenceIdLoader idLoader;
	public final boolean backwardCompatible;

	public ReadJob(BitInputStream input, BitserCache cache, ReferenceIdLoader idLoader, boolean backwardCompatible) {
		this.input = input;
		this.cache = cache;
		this.idLoader = idLoader;
		this.backwardCompatible = backwardCompatible;
	}
}
