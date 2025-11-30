package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.context.ReadContext;
import com.github.knokko.bitser.context.ReadInfo;
import com.github.knokko.bitser.context.WriteContext;
import com.github.knokko.bitser.context.WriteInfo;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.StableReferenceFieldId;
import com.github.knokko.bitser.util.Recursor;
import com.github.knokko.bitser.util.VirtualField;

import java.util.UUID;

import static com.github.knokko.bitser.serialize.IntegerBitser.decodeUniformInteger;
import static com.github.knokko.bitser.serialize.IntegerBitser.encodeUniformInteger;

@BitStruct(backwardCompatible = false)
public class UUIDFieldWrapper extends BitFieldWrapper {

	@BitField
	public final boolean isStableReferenceId;

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
	void readValue(Recursor<ReadContext, ReadInfo> recursor, ValueConsumer setValue) {
		recursor.runFlat("uuid-value", context ->
			setValue.consume(new UUID(
					decodeUniformInteger(Long.MIN_VALUE, Long.MAX_VALUE, context.input),
					decodeUniformInteger(Long.MIN_VALUE, Long.MAX_VALUE, context.input)
			))
		);
	}
}
