package com.github.knokko.bitser;

import com.github.knokko.bitser.field.FloatField;
import com.github.knokko.bitser.io.BitCountStream;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;

import java.io.IOException;

import static com.github.knokko.bitser.IntegerBitser.*;
import static java.lang.Math.abs;

public class FloatBitser {

	public static void encodeFloat(
			double value, boolean doublePrecision, FloatField.Properties field, BitOutputStream output
	) throws IOException {
		if (field.commonValues.length > 0) {
			for (int index = 0; index < field.commonValues.length; index++) {
				if (abs(value - field.commonValues[index]) <= field.errorTolerance) {
					output.prepareProperty("float-common", -1);
					output.write(true);
					output.finishProperty();
					output.prepareProperty("float-common-index", -1);
					encodeUniformInteger(index, 0, field.commonValues.length - 1, output);
					output.finishProperty();
					return;
				}
			}
			output.write(false);
		}
		if (field.expectMultipleOf != 0.0) {
			double doubleValue = ((Number) value).doubleValue();
			long count = Math.round(doubleValue / field.expectMultipleOf);
			double recoveredValue = count * field.expectMultipleOf;

			if (abs(recoveredValue - doubleValue) <= field.errorTolerance) {
				BitCountStream counter = new BitCountStream();
				encodeInteger(count, field.expectedIntegerMultiple, counter);

				if ((!doublePrecision && counter.getCounter() < 32) || (doublePrecision && counter.getCounter() < 64)) {
					output.prepareProperty("float-simplified", -1);
					output.write(true);
					output.finishProperty();
					output.prepareProperty("float-integer-value", -1);
					encodeInteger(count, field.expectedIntegerMultiple, output);
					output.finishProperty();
					return;
				}
			}

			output.prepareProperty("float-simplified", -1);
			output.write(false);
			output.finishProperty();
		}

		output.prepareProperty("float-value", -1);
		if (doublePrecision) {
			encodeFullLong(Double.doubleToRawLongBits(value), output);
		} else {
			encodeFullInt(Float.floatToRawIntBits((float) value), output);
		}
		output.finishProperty();
	}

	public static double decodeFloat(
			boolean doublePrecision, FloatField.Properties field, BitInputStream input
	) throws IOException {
		if (field.commonValues.length > 0 && input.read()) {
			int commonIndex = (int) decodeUniformInteger(0, field.commonValues.length - 1, input);
			return field.commonValues[commonIndex];
		}
		if (field.expectMultipleOf != 0.0 && input.read()) {
			long count = IntegerBitser.decodeInteger(field.expectedIntegerMultiple, input);
			double result = count * field.expectMultipleOf;
			if (doublePrecision) return result;
			else return (float) result;
		}

		if (doublePrecision) {
			return Double.longBitsToDouble(decodeFullLong(input));
		} else {
			return Float.intBitsToFloat(decodeFullInt(input));
		}
	}
}
