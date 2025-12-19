package com.github.knokko.bitser;

import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.StableReferenceFieldId;
import com.github.knokko.bitser.util.Recursor;

import java.util.UUID;
import java.util.function.Consumer;

import static com.github.knokko.bitser.IntegerBitser.decodeFullLong;
import static com.github.knokko.bitser.IntegerBitser.encodeFullLong;

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
			encodeFullLong(value.getMostSignificantBits(), context.output);
			context.output.finishProperty();
			context.output.prepareProperty("least-significant-bits", -1);
			encodeFullLong(value.getLeastSignificantBits(), context.output);
			context.output.finishProperty();
		});
	}

	@Override
	public void write(
			Serializer serializer, Object rawValue,
			RecursionNode parentNode, String fieldName
	) throws Throwable {
		UUID value = (UUID) rawValue;
		serializer.output.prepareProperty("uuid", -1);
		encodeFullLong(value.getMostSignificantBits(), serializer.output);
		encodeFullLong(value.getLeastSignificantBits(), serializer.output);
		serializer.output.finishProperty();
	}

	@Override
	public Object read(Deserializer deserializer, RecursionNode parentNode, String fieldName) throws Throwable {
		deserializer.input.prepareProperty("uuid", -1);
		UUID result = new UUID(decodeFullLong(deserializer.input), decodeFullLong(deserializer.input));
		deserializer.input.finishProperty();
		return result;
	}

	@Override
	void readValue(Recursor<ReadContext, ReadInfo> recursor, Consumer<Object> setValue) {
		recursor.runFlat("uuid-value", context ->
			setValue.accept(new UUID(decodeFullLong(context.input), decodeFullLong(context.input)))
		);
	}
}
