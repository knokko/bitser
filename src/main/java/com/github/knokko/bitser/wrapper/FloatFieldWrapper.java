package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.FloatField;
import com.github.knokko.bitser.io.BitCountStream;
import com.github.knokko.bitser.serialize.IntegerBitser;
import com.github.knokko.bitser.serialize.ReadJob;
import com.github.knokko.bitser.serialize.WriteJob;
import com.github.knokko.bitser.util.VirtualField;

import java.io.IOException;

import static com.github.knokko.bitser.serialize.IntegerBitser.decodeUniformInteger;
import static com.github.knokko.bitser.serialize.IntegerBitser.encodeUniformInteger;
import static java.lang.Math.abs;

@BitStruct(backwardCompatible = false)
public class FloatFieldWrapper extends BitFieldWrapper {

	@BitField
	private final FloatField.Properties floatField;

	FloatFieldWrapper(VirtualField field, FloatField floatField) {
		super(field);
		this.floatField = new FloatField.Properties(floatField);

		Class<?> type = field.type;
		if (type != float.class && type != double.class && type != Float.class && type != Double.class) {
			throw new InvalidBitFieldException("FloatField only supports floats and doubles, but got " + type);
		}
	}

	@Override
	void writeValue(Object value, WriteJob write) throws IOException {
		if (floatField.expectMultipleOf != 0.0) {
			double doubleValue = ((Number) value).doubleValue();
			long count = Math.round(doubleValue / floatField.expectMultipleOf);
			double recoveredValue = count * floatField.expectMultipleOf;

			if (abs(recoveredValue - doubleValue) <= floatField.errorTolerance) {
				BitCountStream counter = new BitCountStream();
				IntegerBitser.encodeVariableInteger(count, Long.MIN_VALUE, Long.MAX_VALUE, counter);

				if ((value instanceof Float && counter.getCounter() < 32) || (value instanceof Double && counter.getCounter() < 64)) {
					write.output.write(true);
					IntegerBitser.encodeVariableInteger(count, Long.MIN_VALUE, Long.MAX_VALUE, write.output);
					return;
				}
			}

			write.output.write(false);
		}

		if (value instanceof Float) {
			encodeUniformInteger(Float.floatToRawIntBits((Float) value), Integer.MIN_VALUE, Integer.MAX_VALUE, write.output);
		} else {
			encodeUniformInteger(Double.doubleToRawLongBits((Double) value), Long.MIN_VALUE, Long.MAX_VALUE, write.output);
		}
	}

	@Override
	void readValue(ReadJob read, ValueConsumer setValue) throws IOException {
		if (floatField.expectMultipleOf != 0.0 && read.input.read()) {
			long count = IntegerBitser.decodeVariableInteger(Long.MIN_VALUE, Long.MAX_VALUE, read.input);
			double result = count * floatField.expectMultipleOf;
			if (field.type == float.class || field.type == Float.class) setValue.consume((float) result);
			else setValue.consume(result);
			return;
		}

		if (field.type == float.class || field.type == Float.class) {
			setValue.consume(Float.intBitsToFloat((int) decodeUniformInteger(Integer.MIN_VALUE, Integer.MAX_VALUE, read.input)));
		} else {
			setValue.consume(Double.longBitsToDouble(decodeUniformInteger(Long.MIN_VALUE, Long.MAX_VALUE, read.input)));
		}
	}
}
