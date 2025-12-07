package com.github.knokko.bitser;

import com.github.knokko.bitser.util.Recursor;

import java.util.function.Consumer;

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
	void collectReferenceLabels(Recursor<LabelContext, LabelInfo> recursor) {
		super.collectReferenceLabels(recursor);
		recursor.runFlat("unstable", context -> context.unstable.add(label));
	}

	@Override
	void writeValue(Object value, Recursor<WriteContext, WriteInfo> recursor) {
		recursor.runFlat("unstable reference", context ->
				context.idMapper.maybeEncodeUnstableId(label, value, context.output, -1)
		);
	}

	@Override
	void readValue(Recursor<ReadContext, ReadInfo> recursor, Consumer<Object> setValue) {
		recursor.runFlat("unstable-id", context ->
				context.idLoader.getUnstable(label, setValue, context.input)
		);
	}
}
