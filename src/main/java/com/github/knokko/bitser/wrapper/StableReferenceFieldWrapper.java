package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.serialize.LabelCollection;
import com.github.knokko.bitser.serialize.ReadJob;
import com.github.knokko.bitser.serialize.WriteJob;
import com.github.knokko.bitser.util.VirtualField;

import java.io.IOException;

@BitStruct(backwardCompatible = false)
class StableReferenceFieldWrapper extends ReferenceFieldWrapper {

	StableReferenceFieldWrapper(VirtualField field, String label) {
		super(field, label);
	}

	@SuppressWarnings("unused")
	private StableReferenceFieldWrapper() {
		super();
	}

	@Override
	public void collectReferenceLabels(LabelCollection labels) {
		super.collectReferenceLabels(labels);
		labels.stable.add(label);
	}

	@Override
	public void collectUsedReferenceLabels(LabelCollection labels, Object value) {
		super.collectReferenceLabels(labels);
		if (value != null) labels.stable.add(label);
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
