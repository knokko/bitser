package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.LegacyBitserException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.StableReferenceFieldId;
import com.github.knokko.bitser.legacy.BackUUIDValue;

import java.util.UUID;

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
	Object read(BackDeserializer deserializer, RecursionNode parentNode, String fieldName) throws Throwable {
		deserializer.input.prepareProperty("uuid", -1);
		UUID result = new UUID(decodeFullLong(deserializer.input), decodeFullLong(deserializer.input));
		deserializer.input.finishProperty();
		return new BackUUIDValue(result);
	}

	@Override
	Object convert(BackDeserializer deserializer, Object legacyValue, RecursionNode parentNode, String fieldName) {
		if (legacyValue instanceof BackUUIDValue) {
			return ((BackUUIDValue) legacyValue).value;
		} else {
			throw new LegacyBitserException("Can't convert from legacy " + legacyValue + " to UUID for field " + field);
		}
	}
}
