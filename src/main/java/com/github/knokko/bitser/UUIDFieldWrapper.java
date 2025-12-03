package com.github.knokko.bitser;

import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.StableReferenceFieldId;
import com.github.knokko.bitser.util.Recursor;

import java.util.UUID;
import java.util.function.Consumer;

import static com.github.knokko.bitser.IntegerBitser.decodeUniformInteger;
import static com.github.knokko.bitser.IntegerBitser.encodeUniformInteger;

@BitStruct(backwardCompatible = false)
class UUIDFieldWrapper extends BitFieldWrapper {

	@BitField
	final boolean isStableReferenceId;

	UUIDFieldWrapper(VirtualField field) {
		super(field);
		this.isStableReferenceId = field.annotations.has(StableReferenceFieldId.class);
	}

	@SuppressWarnings("unused")
	private UUIDFieldWrapper() {
		super();
		this.isStableReferenceId = false;
	}

	@Override
	void writeValue(Object rawValue, Recursor<WriteContext, WriteInfo> recursor) {
		UUID value = (UUID) rawValue;

		recursor.runFlat("uuid", context -> {
			context.output.prepareProperty("most-significant-bits", -1);
			encodeUniformInteger(value.getMostSignificantBits(), Long.MIN_VALUE, Long.MAX_VALUE, context.output);
			context.output.finishProperty();
			context.output.prepareProperty("least-significant-bits", -1);
			encodeUniformInteger(value.getLeastSignificantBits(), Long.MIN_VALUE, Long.MAX_VALUE, context.output);
			context.output.finishProperty();
		});
	}

	@Override
	void readValue(Recursor<ReadContext, ReadInfo> recursor, Consumer<Object> setValue) {
		recursor.runFlat("uuid-value", context ->
			setValue.accept(new UUID(
					decodeUniformInteger(Long.MIN_VALUE, Long.MAX_VALUE, context.input),
					decodeUniformInteger(Long.MIN_VALUE, Long.MAX_VALUE, context.input)
			))
		);
	}
}
