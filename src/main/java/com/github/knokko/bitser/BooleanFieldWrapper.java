package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.LegacyBitserException;
import com.github.knokko.bitser.legacy.BackBooleanValue;
import com.github.knokko.bitser.util.Recursor;

import java.util.function.Consumer;

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
	public void write(
			Serializer serializer, Object value,
			RecursionNode parentNode, String fieldName
	) throws Throwable {
		serializer.output.prepareProperty("boolean-value", -1);
		serializer.output.write((Boolean) value);
		serializer.output.finishProperty();
	}

	@Override
	public Object read(Deserializer deserializer, RecursionNode parentNode, String fieldName) throws Throwable {
		deserializer.input.prepareProperty("boolean-value", -1);
		boolean result = deserializer.input.read();
		deserializer.input.finishProperty();
		return result;
	}

	@Override
	Object read(BackDeserializer deserializer, RecursionNode parentNode, String fieldName) throws Throwable {
		deserializer.input.prepareProperty("boolean-value", -1);
		boolean result = deserializer.input.read();
		deserializer.input.finishProperty();
		return result ? BackBooleanValue.TRUE : BackBooleanValue.FALSE;
	}

	@Override
	Object convert(BackDeserializer deserializer, Object legacyValue, RecursionNode parentNode, String fieldName) {
		if (legacyValue instanceof BackBooleanValue) {
			return ((BackBooleanValue) legacyValue).value;
		} else {
			throw new LegacyBitserException("Can't convert from legacy " + legacyValue + " to boolean for field " + field);
		}
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
	void readValue(Recursor<ReadContext, ReadInfo> recursor, Consumer<Object> setValue) {
		recursor.runFlat("boolean-value", context -> setValue.accept(context.input.read()));
	}
}
