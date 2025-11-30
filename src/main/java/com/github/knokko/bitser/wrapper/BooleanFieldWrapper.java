package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.context.ReadContext;
import com.github.knokko.bitser.context.ReadInfo;
import com.github.knokko.bitser.context.WriteContext;
import com.github.knokko.bitser.context.WriteInfo;
import com.github.knokko.bitser.util.Recursor;
import com.github.knokko.bitser.util.VirtualField;

@BitStruct(backwardCompatible = false)
class BooleanFieldWrapper extends BitFieldWrapper {

	BooleanFieldWrapper(VirtualField field) {
		super(field);
	}

	@SuppressWarnings("unused")
	private BooleanFieldWrapper() {
		super();
	}

	@Override
	void writeValue(Object value, Recursor<WriteContext, WriteInfo> recursor) {
		recursor.runFlat("boolean-value", context -> {
			context.output.prepareProperty("boolean-value", -1);
			context.output.write((Boolean) value);
			context.output.finishProperty();
		});
	}

	@Override
	void readValue(Recursor<ReadContext, ReadInfo> recursor, ValueConsumer setValue) {
		recursor.runFlat("boolean-value", context -> setValue.consume(context.input.read()));
	}
}
