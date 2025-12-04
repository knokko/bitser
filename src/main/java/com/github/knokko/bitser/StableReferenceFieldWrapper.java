package com.github.knokko.bitser;

import com.github.knokko.bitser.util.Recursor;

import java.util.function.Consumer;

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
	void collectReferenceLabels(Recursor<LabelContext, LabelInfo> recursor) {
		super.collectReferenceLabels(recursor);
		recursor.runFlat("stable", labels -> labels.stable.add(label));
	}

	@Override
	void writeValue(Object value, Recursor<WriteContext, WriteInfo> recursor) {
		recursor.runFlat("stable reference", context ->
				context.idMapper.encodeStableId(label, value, context.output, recursor.info.bitser.cache)
		);
	}

	@Override
	void readValue(Recursor<ReadContext, ReadInfo> recursor, Consumer<Object> setValue) {
		recursor.runFlat("stable-reference", context ->
				context.idLoader.getStable(label, setValue, context.input)
		);
	}
}
