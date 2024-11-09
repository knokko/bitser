package com.github.knokko.bitser.serialize;

import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;

import java.io.IOException;

import static java.lang.Long.max;

public class IntegerBitser {

	static int requiredNumberOfBitsForEncoding(long minValue, long maxValue) {
		long maxRange = maxValue - minValue;

		// Overflow check ripped from Math.subtractExact
		boolean overflowed = ((maxValue ^ minValue) & (maxValue ^ maxRange)) < 0;
		if (overflowed) return 64;

		return 64 - Long.numberOfLeadingZeros(maxRange);
	}

	private static void check(long value, long minValue, long maxValue) {
		if (value < minValue || value > maxValue) {
			// TODO Create proper exception for this
			throw new Error(value + " is out of range [" + minValue + ", " + maxValue + "]");
		}
	}

	public static void encodeUniformInteger(long value, long minValue, long maxValue, BitOutputStream output) throws IOException {
		check(value, minValue, maxValue);

		int numBits = requiredNumberOfBitsForEncoding(minValue, maxValue);

		if (numBits == 64) {
			for (int bit = 63; bit >= 0; bit--) output.write((value & (1L << bit)) != 0);
			return;
		}

		value -= minValue;
		for (int bit = numBits - 1; bit >= 0; bit--) output.write((value & (1L << bit)) != 0);
	}

	public static long decodeUniformInteger(long minValue, long maxValue, BitInputStream input) throws IOException {
		int numBits = requiredNumberOfBitsForEncoding(minValue, maxValue);

		if (numBits == 64) {
			long value = 0;
			for (int bit = 63; bit >= 0; bit--) if (input.read()) value |= 1L << bit;
			return value;
		}

		long value = 0;
		for (int bit = numBits - 1; bit >= 0; bit--) {
			if (input.read()) value |= 1L << bit;
		}
		return value + minValue;
	}

	public static long decodeVariableInteger(long minValue, long maxValue, BitInputStream input) throws IOException {
		int maxValueBits;
		if (minValue >= 0) {
			maxValueBits = requiredNumberOfBitsForEncoding(minValue, maxValue);
		} else if (maxValue <= 0) {
			maxValueBits = requiredNumberOfBitsForEncoding(-maxValue, minValue == Long.MIN_VALUE ? -(minValue + 1) : -minValue);
		} else {
			maxValueBits = requiredNumberOfBitsForEncoding(0, max(maxValue, -(minValue + 1)));
		}

		long result = 0;
		for (int bitCounter = 0; bitCounter < maxValueBits; bitCounter++) {
			if (bitCounter == 1 || bitCounter == 4 || bitCounter == 10 || bitCounter == 20 || bitCounter == 40) {
				if (!input.read()) break;
			}

			result <<= 1;
			if (input.read()) result |= 1;
		}

		if (minValue >= 0) {
			result += minValue;
		} else if (maxValue <= 0) {
			result = -result;
			result += maxValue;
		} else {
			if (input.read()) result = -result - 1;
		}

		if (result == -Long.MAX_VALUE && maxValue == 0 && minValue == Long.MIN_VALUE) {
			if (input.read()) return Long.MIN_VALUE;
		}

		return result;
	}

	public static void encodeVariableInteger(long value, long minValue, long maxValue, BitOutputStream output) throws IOException {
		check(value, minValue, maxValue);

		boolean isNegative = value < 0;
		boolean wasMinValue = value == Long.MIN_VALUE;
		boolean wasAlmostMinValue = wasMinValue || value == Long.MIN_VALUE + 1;

		int maxValueBits;
		int valueBits;
		if (minValue >= 0) {
			maxValueBits = requiredNumberOfBitsForEncoding(minValue, maxValue);
			valueBits = requiredNumberOfBitsForEncoding(minValue, value);
			value -= minValue;
		} else if (maxValue <= 0) {
			if (value == Long.MIN_VALUE) value++;
			maxValueBits = requiredNumberOfBitsForEncoding(-maxValue, minValue == Long.MIN_VALUE ? -(minValue + 1) : -minValue);
			value = -value;
			valueBits = requiredNumberOfBitsForEncoding(-maxValue, value);
			value += maxValue;
		} else {
			maxValueBits = requiredNumberOfBitsForEncoding(0, max(maxValue, -(minValue + 1)));
			if (value < 0) value = -(value + 1);
			valueBits = requiredNumberOfBitsForEncoding(0, value);
		}

		int terminatorBit = maxValueBits;
		if (valueBits <= 1) terminatorBit = 1;
		else if (valueBits <= 4) terminatorBit = 4;
		else if (valueBits <= 10) terminatorBit = 10;
		else if (valueBits <= 20) terminatorBit = 20;
		else if (valueBits <= 40) terminatorBit = 40;

		if (terminatorBit > maxValueBits) terminatorBit = maxValueBits;

		int encodedBits = 0;
		for (int valueBit = terminatorBit - 1; valueBit >= 0; valueBit--) {
			output.write((value & (1L << valueBit)) != 0);

			encodedBits++;
			if (valueBit > 0 && (encodedBits == 1 || encodedBits == 4 || encodedBits == 10 || encodedBits == 20 || encodedBits == 40)) {
				output.write(true);
			}
		}

		if (terminatorBit < maxValueBits) output.write(false);
		if (minValue < 0 && maxValue > 0) output.write(isNegative);

		if (maxValue == 0 && minValue == Long.MIN_VALUE && wasAlmostMinValue) output.write(wasMinValue);
	}
}
