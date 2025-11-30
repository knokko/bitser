package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.context.ReadContext;
import com.github.knokko.bitser.context.ReadInfo;
import com.github.knokko.bitser.context.WriteContext;
import com.github.knokko.bitser.context.WriteInfo;
import com.github.knokko.bitser.serialize.LabelCollection;
import com.github.knokko.bitser.util.Recursor;
import com.github.knokko.bitser.util.VirtualField;

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
	void writeValue(Object value, Recursor<WriteContext, WriteInfo> recursor) {
		recursor.runFlat("unstable reference", context ->
				context.idMapper.maybeEncodeUnstableId(label, value, context.output)
		);
	}

	@Override
	void readValue(Recursor<ReadContext, ReadInfo> recursor, ValueConsumer setValue) {
		recursor.runFlat("unstable-id", context ->
				context.idLoader.getUnstable(label, setValue, context.input)
		);
	}
}
