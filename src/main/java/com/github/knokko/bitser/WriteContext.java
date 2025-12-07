package com.github.knokko.bitser;

import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.util.FloatDistributionTracker;
import com.github.knokko.bitser.util.IntegerDistributionTracker;

import java.util.Objects;

class WriteContext {

	final BitOutputStream output;
	final ReferenceIdMapper idMapper;
	final IntegerDistributionTracker integerDistribution;
	final FloatDistributionTracker floatDistribution;

	WriteContext(
			BitOutputStream output, ReferenceIdMapper idMapper,
			IntegerDistributionTracker integerDistribution,
			FloatDistributionTracker floatDistribution
	) {
		this.output = Objects.requireNonNull(output);
		this.idMapper = Objects.requireNonNull(idMapper);
		this.integerDistribution = integerDistribution;
		this.floatDistribution = floatDistribution;
	}
}
