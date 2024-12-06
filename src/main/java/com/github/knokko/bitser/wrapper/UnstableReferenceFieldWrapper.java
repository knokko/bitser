package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.BitserCache;
import com.github.knokko.bitser.util.ReferenceIdLoader;
import com.github.knokko.bitser.util.ReferenceIdMapper;
import com.github.knokko.bitser.util.VirtualField;

import java.io.IOException;
import java.util.Set;

class UnstableReferenceFieldWrapper extends BitFieldWrapper {

	private final String label;

	UnstableReferenceFieldWrapper(VirtualField field, String label) {
		super(field);
		this.label = label;
	}

	@Override
	void collectReferenceTargetLabels(
			BitserCache cache, Set<String> declaredTargetLabels,
			Set<String> stableLabels, Set<String> unstableLabels, Set<Object> visitedObjects
	) {
		super.collectReferenceTargetLabels(cache, declaredTargetLabels, stableLabels, unstableLabels, visitedObjects);
		unstableLabels.add(label);
	}

	@Override
	void writeValue(
			Object value, BitOutputStream output, BitserCache cache, ReferenceIdMapper idMapper
	) throws IOException {
		idMapper.maybeEncodeUnstableId(label, value, output);
	}

	@Override
	void readValue(
			BitInputStream input, BitserCache cache, ReferenceIdLoader idLoader, ValueConsumer setValue
	) throws IOException {
		idLoader.getUnstable(label, setValue, input);
	}
}
