package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.BitserCache;
import com.github.knokko.bitser.util.ReferenceIdLoader;
import com.github.knokko.bitser.util.ReferenceIdMapper;
import com.github.knokko.bitser.util.VirtualField;

import java.io.IOException;
import java.util.Set;

class StructFieldWrapper extends BitFieldWrapper {

	StructFieldWrapper(VirtualField field) {
		super(field);
	}

	@Override
	void collectReferenceTargetLabels(
			BitserCache cache, Set<String> declaredTargetLabels,
			Set<String> stableLabels, Set<String> unstableLabels, Set<Object> visitedObjects
	) {
		super.collectReferenceTargetLabels(cache, declaredTargetLabels, stableLabels, unstableLabels, visitedObjects);
		cache.getWrapper(field.type).collectReferenceTargetLabels(
				cache, declaredTargetLabels, stableLabels, unstableLabels, visitedObjects
		);
	}

	@Override
	void registerReferenceTargets(Object value, BitserCache cache, ReferenceIdMapper idMapper) {
		super.registerReferenceTargets(value, cache, idMapper);
		if (value != null) cache.getWrapper(field.type).registerReferenceTargets(value, cache, idMapper);
	}

	@Override
	void writeValue(Object value, BitOutputStream output, BitserCache cache, ReferenceIdMapper idMapper) throws IOException {
		cache.getWrapper(field.type).write(value, output, cache, idMapper);
	}

	@Override
	void readValue(
			BitInputStream input, BitserCache cache, ReferenceIdLoader idLoader, ValueConsumer setValue
	) throws IOException {
		cache.getWrapper(field.type).read(input, cache, idLoader, setValue);
	}
}
