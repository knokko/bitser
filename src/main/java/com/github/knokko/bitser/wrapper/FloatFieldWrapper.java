package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.context.ReadContext;
import com.github.knokko.bitser.context.ReadInfo;
import com.github.knokko.bitser.context.WriteContext;
import com.github.knokko.bitser.context.WriteInfo;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.FloatField;
import com.github.knokko.bitser.io.BitCountStream;
import com.github.knokko.bitser.serialize.IntegerBitser;
import com.github.knokko.bitser.util.Recursor;
import com.github.knokko.bitser.util.VirtualField;

import java.util.function.Consumer;

import static com.github.knokko.bitser.serialize.IntegerBitser.*;
import static java.lang.Math.abs;

@BitStruct(backwardCompatible = false)
public class FloatFieldWrapper extends BitFieldWrapper {

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
			if (floatField.expectMultipleOf != 0.0) {
				double doubleValue = ((Number) value).doubleValue();
				long count = Math.round(doubleValue / floatField.expectMultipleOf);
				double recoveredValue = count * floatField.expectMultipleOf;

				if (abs(recoveredValue - doubleValue) <= floatField.errorTolerance) {
					BitCountStream counter = new BitCountStream();
					encodeVariableInteger(count, Long.MIN_VALUE, Long.MAX_VALUE, counter);

					if ((value instanceof Float && counter.getCounter() < 32) || (value instanceof Double && counter.getCounter() < 64)) {
						context.output.prepareProperty("float-simplified", -1);
						context.output.write(true);
						context.output.finishProperty();
						context.output.prepareProperty("float-integer-value", -1);
						encodeVariableInteger(count, Long.MIN_VALUE, Long.MAX_VALUE, context.output);
						context.output.finishProperty();
						return;
					}
				}

				context.output.prepareProperty("float-simplified", -1);
				context.output.write(false);
				context.output.finishProperty();
			}

			context.output.prepareProperty("float-value", -1);
			if (value instanceof Float) {
				encodeUniformInteger(Float.floatToRawIntBits((Float) value), Integer.MIN_VALUE, Integer.MAX_VALUE, context.output);
			} else {
				encodeUniformInteger(Double.doubleToRawLongBits((Double) value), Long.MIN_VALUE, Long.MAX_VALUE, context.output);
			}
			context.output.finishProperty();
		});
	}

	@Override
	void readValue(Recursor<ReadContext, ReadInfo> recursor, ValueConsumer setValue) {
		recursor.runFlat("float-value", context -> {
			if (floatField.expectMultipleOf != 0.0 && context.input.read()) {
				long count = IntegerBitser.decodeVariableInteger(Long.MIN_VALUE, Long.MAX_VALUE, context.input);
				double result = count * floatField.expectMultipleOf;
				if (isFloat) setValue.consume((float) result);
				else setValue.consume(result);
				return;
			}

			if (isFloat) {
				setValue.consume(Float.intBitsToFloat((int) decodeUniformInteger(Integer.MIN_VALUE, Integer.MAX_VALUE, context.input)));
			} else {
				setValue.consume(Double.longBitsToDouble(decodeUniformInteger(Long.MIN_VALUE, Long.MAX_VALUE, context.input)));
			}
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
			throw new InvalidBitValueException("Can't convert from legacy " + value + " to " + field.type + " for field " + field);
		}
	}
}
