package com.github.knokko.bitser;

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
		serializer.output.write((Boolean) value);
	}

	@Override
	public Object read(Deserializer deserializer, RecursionNode parentNode, String fieldName) throws Throwable {
		return deserializer.input.read();
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
