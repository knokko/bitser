package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.options.CollectionSizeLimit;

import java.io.IOException;

import static java.lang.Long.max;

public class IntegerBitser {

	public static int requiredNumberOfBitsForEncoding(long minValue, long maxValue) {
		long maxRange = maxValue - minValue;

		// Overflow check ripped from Math.subtractExact
		boolean overflowed = ((maxValue ^ minValue) & (maxValue ^ maxRange)) < 0;
		if (overflowed) return 64;

		return 64 - Long.numberOfLeadingZeros(maxRange);
	}

	private static void check(long value, long minValue, long maxValue) {
		if (value < minValue || value > maxValue) {
			throw new InvalidBitValueException(value + " is out of range [" + minValue + ", " + maxValue + "]");
		}
	}

	public static void encodeInteger(
			long value, IntegerField.Properties field, BitOutputStream output
	) throws IOException {
		if (field.commonValues.length > 0) {
			int commonIndex = -1;
			for (int index = 0; index < field.commonValues.length; index++) {
				if (value == field.commonValues[index]) commonIndex = index;
			}

			output.write(commonIndex != -1);
			if (commonIndex != -1) {
				encodeUniformInteger(commonIndex, 0, field.commonValues.length - 1, output);
				return;
			}
		}
		if (field.expectUniform) {
			encodeUniformInteger(value, field.minValue, field.maxValue, output);
		} else {
			if (field.digitSize == IntegerField.DIGIT_SIZE_TERMINATORS) {
				encodeVariableIntegerUsingTerminatorBits(value, field.minValue, field.maxValue, output);
			} else if (field.digitSize >= 2 && field.digitSize <= 7) {
				encodeDigitInteger(value, field.minValue, field.maxValue, field.digitSize, output);
			} else {
				throw new InvalidBitFieldException("Unsupported digit size " + field.digitSize);
			}
		}
	}

	public static void encodeUnknownLength(int length, BitOutputStream output) throws IOException {
		encodeVariableIntegerUsingTerminatorBits(length, 0L, Integer.MAX_VALUE, output);
	}

	public static void encodeFullLong(long value, BitOutputStream output) throws IOException {
		output.write((int) value & 255, 8);
		output.write((int) (value >> 8) & 255, 8);
		output.write((int) (value >> 16) & 255, 8);
		output.write((int) (value >> 24) & 255, 8);
		output.write((int) (value >> 32) & 255, 8);
		output.write((int) (value >> 40) & 255, 8);
		output.write((int) (value >> 48) & 255, 8);
		output.write((int) (value >> 56) & 255, 8);
	}

	public static void encodeFullInt(int value, BitOutputStream output) throws IOException {
		output.write(value & 255, 8);
		output.write((value >> 8) & 255, 8);
		output.write((value >> 16) & 255, 8);
		output.write((value >> 24) & 255, 8);
	}

	public static void encodeUniformInteger(long value, long minValue, long maxValue, BitOutputStream output) throws IOException {
		check(value, minValue, maxValue);

		int numBits = requiredNumberOfBitsForEncoding(minValue, maxValue);

		if (numBits == 64) {
			encodeFullLong(value, output);
			return;
		}

		value -= minValue;
		int remainingBits = numBits;
		for (; remainingBits > 8; remainingBits -= 8) {
			output.write((int) value & 255, 8);
			value >>= 8;
		}
		output.write((int) value & 255, remainingBits);
	}

	public static void encodeDigitInteger(long value, long minValue, long maxValue, int digitSize, BitOutputStream output) throws IOException {
		check(value, minValue, maxValue);
		int numDigits = (1 << digitSize) - 1;

		if (minValue < 0L && maxValue >= 0L) output.write(value >= 0L);
		long maxPositiveValue;
		if (value < 0L) {
			value = -(value + 1L);
			maxPositiveValue = -(minValue + 1L);
			if (maxValue < 0L) {
				value += maxValue + 1;
				maxPositiveValue += maxValue + 1;
			}
		} else {
			maxPositiveValue = maxValue;
			if (minValue > 0L) {
				value -= minValue;
				maxPositiveValue -= minValue;
			}
		}

		while (value > 0) {
			output.write((byte) (value % numDigits), digitSize);
			value /= numDigits;
			maxPositiveValue /= numDigits;
		}
		if (maxPositiveValue > 0L) {
			output.write((byte) numDigits, digitSize);
		}
	}

	public static long decodeInteger(IntegerField.Properties field, BitInputStream input) throws IOException {
		if (field.commonValues.length > 0 && input.read()) {
			int commonIndex = (int) decodeUniformInteger(0, field.commonValues.length - 1, input);
			return field.commonValues[commonIndex];
		}
		if (field.expectUniform) {
			return decodeUniformInteger(field.minValue, field.maxValue, input);
		} else {
			if (field.digitSize == IntegerField.DIGIT_SIZE_TERMINATORS) {
				return decodeVariableIntegerUsingTerminatorBits(field.minValue, field.maxValue, input);
			} else if (field.digitSize >= 2 && field.digitSize <= 7) {
				return decodeDigitInteger(field.minValue, field.maxValue, field.digitSize, input);
			} else {
				throw new InvalidBitFieldException("Unsupported digit size " + field.digitSize);
			}
		}
	}

	public static int decodeLength(
			IntegerField.Properties sizeField, CollectionSizeLimit sizeLimit, String description, BitInputStream input
	) throws IOException {
		int length = (int) decodeInteger(sizeField, input);
		if (sizeLimit != null && length > sizeLimit.maxSize) {
			throw new InvalidBitValueException(
					description + " " + length + " exceeds the size limit of " + sizeLimit.maxSize
			);
		}
		return length;
	}

	public static int decodeUnknownLength(
			CollectionSizeLimit sizeLimit, String description, BitInputStream input
	) throws IOException {
		int length = (int) decodeVariableIntegerUsingTerminatorBits(0L, Integer.MAX_VALUE, input);
		if (sizeLimit != null && length > sizeLimit.maxSize) {
			throw new InvalidBitValueException(
					description + " " + length + " exceeds the size limit of " + sizeLimit.maxSize
			);
		}
		return length;
	}

	public static long decodeFullLong(BitInputStream input) throws IOException {
		return input.read(8) | ((long) input.read(8) << 8) | ((long) input.read(8) << 16) |
				((long) input.read(8) << 24) | ((long) input.read(8) << 32) |
				((long) input.read(8) << 40) | ((long) input.read(8) << 48) |
				((long) input.read(8) << 56);
	}

	public static int decodeFullInt(BitInputStream input) throws IOException {
		return input.read(8) | (input.read(8) << 8) | (input.read(8) << 16) |
				(input.read(8) << 24);
	}

	public static long decodeUniformInteger(long minValue, long maxValue, BitInputStream input) throws IOException {
		int numBits = requiredNumberOfBitsForEncoding(minValue, maxValue);

		if (numBits == 64) return decodeFullLong(input);

		long value = 0;
		int startBit = 0;
		for (; startBit + 8 < numBits; startBit += 8) {
			value |= (long) (input.read(8)) << startBit;
		}
		value |= (long) (input.read(numBits - startBit)) << startBit;
		return value + minValue;
	}

	public static long decodeDigitInteger(
			long minValue, long maxValue, int digitSize, BitInputStream input
	) throws IOException {
		int numDigits = (1 << digitSize) - 1;

		boolean checkSign = minValue < 0L && maxValue >= 0L;
		boolean isPositive;
		long maxPositiveValue;
		if (checkSign) {
			if (input.read()) {
				isPositive = true;
				maxPositiveValue = maxValue;
			} else {
				isPositive = false;
				maxPositiveValue = -(minValue + 1L);
			}
		} else {
			isPositive = minValue >= 0L;
			maxPositiveValue = maxValue - minValue;
		}

		long value = 0L;
		long optimisticValue = 0L;
		long digitWeight = 1L;
		while (true) {
			if (optimisticValue >= maxPositiveValue) break;
			int nextDigit = input.read(digitSize);
			if (nextDigit == numDigits) break;

			value += digitWeight * nextDigit;

			long addOptimistic = digitWeight * (numDigits - 1);
			long oldOptimistic = optimisticValue;
			optimisticValue += addOptimistic;

			// If optimisticValue overflows, we know for sure that no more digits are needed
			if (optimisticValue < oldOptimistic || addOptimistic / digitWeight != numDigits - 1) break;

			digitWeight *= numDigits;
		}

		if (checkSign) {
			if (isPositive) return value;
			else return -value - 1L;
		} else {
			if (isPositive) return value + minValue;
			else return -value + maxValue;
		}
	}

	public static long decodeVariableIntegerUsingTerminatorBits(long minValue, long maxValue, BitInputStream input) throws IOException {
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

	public static void encodeVariableIntegerUsingTerminatorBits(long value, long minValue, long maxValue, BitOutputStream output) throws IOException {
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
