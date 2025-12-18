package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.LegacyBitserException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.FloatField;
import com.github.knokko.bitser.util.Recursor;

import java.util.function.Consumer;

import static com.github.knokko.bitser.FloatBitser.decodeFloat;
import static com.github.knokko.bitser.FloatBitser.encodeFloat;

@BitStruct(backwardCompatible = false)
class FloatFieldWrapper extends BitFieldWrapper {

	@BitField
	private final FloatField.Properties floatField;

	@BitField
	private final boolean isFloat;

	FloatFieldWrapper(VirtualField field, FloatField floatField) {
		super(field);
		this.floatField = new FloatField.Properties(floatField);

		Class<?> type = field.type;
		if (type != float.class && type != double.class && type != Float.class && type != Double.class) {
			throw new InvalidBitFieldException("FloatField only supports floats and doubles, but got " + type);
		}
		this.isFloat = type == float.class || type == Float.class;
	}

	@SuppressWarnings("unused")
	private FloatFieldWrapper() {
		super();
		this.floatField = new FloatField.Properties();
		this.isFloat = false;
	}

	@Override
	void writeValue(Object value, Recursor<WriteContext, WriteInfo> recursor) {
		recursor.runFlat("float-value", context -> {
			if (context.floatDistribution != null) {
				context.floatDistribution.insert(field.toString(), ((Number) value).doubleValue(), floatField);
			}
			encodeFloat(((Number) value).doubleValue(), value instanceof Double, floatField, context.output);
		});
	}

	@Override
	public void write(
			Serializer serializer, Object value,
			RecursionNode parentNode, String fieldName
	) throws Throwable {
		encodeFloat(((Number) value).doubleValue(), !isFloat, floatField, serializer.output);
	}

	@Override
	public Object read(Deserializer deserializer, RecursionNode parentNode, String fieldName) throws Throwable {
		return decodeFloat(!isFloat, floatField, deserializer.input);
	}

	@Override
	void readValue(Recursor<ReadContext, ReadInfo> recursor, Consumer<Object> setValue) {
		recursor.runFlat("float-value", context -> {
			double value = decodeFloat(!isFloat, floatField, context.input);
			if (isFloat) setValue.accept((float) value);
			else setValue.accept(value);
		});
	}

	@Override
	void setLegacyValue(Recursor<ReadContext, ReadInfo> recursor, Object value, Consumer<Object> setValue) {
		if (value == null) {
			super.setLegacyValue(recursor, null, setValue);
		} else if (value instanceof Number) {
			double d = ((Number) value).doubleValue();
			if (isFloat) super.setLegacyValue(recursor, (float) d, setValue);
			else super.setLegacyValue(recursor, d, setValue);
		} else {
			throw new LegacyBitserException("Can't convert from legacy " + value + " to " + field.type + " for field " + field);
		}
	}
}
