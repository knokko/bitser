package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.serialize.BitserCache;
import com.github.knokko.bitser.serialize.ReadJob;
import com.github.knokko.bitser.serialize.WriteJob;
import com.github.knokko.bitser.util.VirtualField;

import java.io.IOException;
import java.util.Set;

@BitStruct(backwardCompatible = false)
class UnstableReferenceFieldWrapper extends BitFieldWrapper {

	@BitField
	private final String label;

	UnstableReferenceFieldWrapper(VirtualField field, String label) {
		super(field);
		this.label = label;
	}

	@Override
	void collectReferenceTargetLabels(
			BitserCache cache, Set<String> declaredTargetLabels,
			Set<String> stableLabels, Set<String> unstableLabels, Set<BitserWrapper<?>> visitedStructs
	) {
		super.collectReferenceTargetLabels(cache, declaredTargetLabels, stableLabels, unstableLabels, visitedStructs);
		unstableLabels.add(label);
	}

	@Override
	void writeValue(Object value, WriteJob write) throws IOException {
		write.idMapper.maybeEncodeUnstableId(label, value, write.output);
	}

	@Override
	void readValue(ReadJob read, ValueConsumer setValue) throws IOException {
		read.idLoader.getUnstable(label, setValue, read.input);
	}
}
