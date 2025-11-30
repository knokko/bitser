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
	void writeValue(Object value, Recursor<WriteContext, WriteInfo> recursor) {
		recursor.runFlat("stable reference", context ->
				context.idMapper.encodeStableId(label, value, context.output, recursor.info.bitser.cache)
		);
	}

	@Override
	void readValue(Recursor<ReadContext, ReadInfo> recursor, ValueConsumer setValue) {
		recursor.runFlat("stable-reference", context ->
				context.idLoader.getStable(label, setValue, context.input)
		);
	}
}
