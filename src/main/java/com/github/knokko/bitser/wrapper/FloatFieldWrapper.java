package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.FloatField;
import com.github.knokko.bitser.io.BitCountStream;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.BitserCache;
import com.github.knokko.bitser.serialize.IntegerBitser;
import com.github.knokko.bitser.util.ReferenceIdLoader;
import com.github.knokko.bitser.util.ReferenceIdMapper;

import java.io.IOException;
import java.lang.reflect.Field;

import static com.github.knokko.bitser.serialize.IntegerBitser.decodeUniformInteger;
import static com.github.knokko.bitser.serialize.IntegerBitser.encodeUniformInteger;
import static java.lang.Math.abs;

public class FloatFieldWrapper extends BitFieldWrapper {

	private final FloatField floatField;

	FloatFieldWrapper(BitField.Properties properties, FloatField floatField, Field classField) {
		super(properties, classField);
		this.floatField = floatField;

		Class<?> type = properties.type;
		if (type != float.class && type != double.class && type != Float.class && type != Double.class) {
			throw new InvalidBitFieldException("FloatField only supports floats and doubles, but got " + type);
		}
	}

	@Override
	void writeValue(Object value, BitOutputStream output, BitserCache cache, ReferenceIdMapper idMapper) throws IOException {
		if (floatField.expectMultipleOf() != 0.0) {
			double doubleValue = ((Number) value).doubleValue();
			long count = Math.round(doubleValue / floatField.expectMultipleOf());
			double recoveredValue = count * floatField.expectMultipleOf();

			if (abs(recoveredValue - doubleValue) <= floatField.errorTolerance()) {
				BitCountStream counter = new BitCountStream();
				IntegerBitser.encodeVariableInteger(count, Long.MIN_VALUE, Long.MAX_VALUE, counter);

				if ((value instanceof Float && counter.getCounter() < 32) || (value instanceof Double && counter.getCounter() < 64)) {
					output.write(true);
					IntegerBitser.encodeVariableInteger(count, Long.MIN_VALUE, Long.MAX_VALUE, output);
					return;
				}
			}

			output.write(false);
		}

		if (value instanceof Float) {
			encodeUniformInteger(Float.floatToRawIntBits((Float) value), Integer.MIN_VALUE, Integer.MAX_VALUE, output);
		} else {
			encodeUniformInteger(Double.doubleToRawLongBits((Double) value), Long.MIN_VALUE, Long.MAX_VALUE, output);
		}
	}

	@Override
	void readValue(
			BitInputStream input, BitserCache cache, ReferenceIdLoader idLoader, ValueConsumer setValue
	) throws IOException, IllegalAccessException {
		if (floatField.expectMultipleOf() != 0.0 && input.read()) {
			long count = IntegerBitser.decodeVariableInteger(Long.MIN_VALUE, Long.MAX_VALUE, input);
			double result = count * floatField.expectMultipleOf();
			if (properties.type == float.class || properties.type == Float.class) setValue.consume((float) result);
			else setValue.consume(result);
			return;
		}

		if (properties.type == float.class || properties.type == Float.class) {
			setValue.consume(Float.intBitsToFloat((int) decodeUniformInteger(Integer.MIN_VALUE, Integer.MAX_VALUE, input)));
		} else {
			setValue.consume(Double.longBitsToDouble(decodeUniformInteger(Long.MIN_VALUE, Long.MAX_VALUE, input)));
		}
	}
}
