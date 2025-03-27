package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.serialize.LabelCollection;
import com.github.knokko.bitser.serialize.ReadJob;
import com.github.knokko.bitser.serialize.WriteJob;
import com.github.knokko.bitser.util.VirtualField;

import java.io.IOException;

@BitStruct(backwardCompatible = false)
class UnstableReferenceFieldWrapper extends ReferenceFieldWrapper {

	UnstableReferenceFieldWrapper(VirtualField field, String label) {
		super(field, label);
	}

	@SuppressWarnings("unused")
	private UnstableReferenceFieldWrapper() {
		super();
	}

	@Override
	public void collectReferenceLabels(LabelCollection labels) {
		super.collectReferenceLabels(labels);
		labels.unstable.add(label);
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
