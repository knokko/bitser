package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.serialize.BitserCache;
import com.github.knokko.bitser.serialize.ReadJob;
import com.github.knokko.bitser.serialize.WriteJob;
import com.github.knokko.bitser.util.VirtualField;

import java.io.IOException;
import java.util.Set;

class StableReferenceFieldWrapper extends BitFieldWrapper {

	private final String label;

	StableReferenceFieldWrapper(VirtualField field, String label) {
		super(field);
		this.label = label;
	}

	@Override
	void collectReferenceTargetLabels(
			BitserCache cache, Set<String> declaredTargetLabels,
			Set<String> stableLabels, Set<String> unstableLabels, Set<BitserWrapper<?>> visitedStructs
	) {
		super.collectReferenceTargetLabels(cache, declaredTargetLabels, stableLabels, unstableLabels, visitedStructs);
		stableLabels.add(label);
	}

	@Override
	void writeValue(Object value, WriteJob write) throws IOException {
		write.idMapper.encodeStableId(label, value, write.output, write.cache);
	}

	@Override
	void readValue(ReadJob read, ValueConsumer setValue) throws IOException {
		read.idLoader.getStable(label, setValue, read.input);
	}
}
