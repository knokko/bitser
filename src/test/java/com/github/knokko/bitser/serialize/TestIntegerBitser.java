package com.github.knokko.bitser.serialize;

import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.io.BitStringStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingConsumer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import static com.github.knokko.bitser.serialize.IntegerBitser.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestIntegerBitser {

	@Test
	public void testRequiredNumberOfBitsForEncoding() {
		for (long offset : new long[]{Long.MIN_VALUE, -100, 0, 100, Long.MAX_VALUE - 4}) {
			assertEquals(1, requiredNumberOfBitsForEncoding(offset, offset + 1));
			assertEquals(2, requiredNumberOfBitsForEncoding(offset, offset + 2));
			assertEquals(2, requiredNumberOfBitsForEncoding(offset, offset + 3));
			assertEquals(3, requiredNumberOfBitsForEncoding(offset, offset + 4));
		}

		for (long value : new long[]{Long.MIN_VALUE, -100, 0, 100, Long.MAX_VALUE}) {
			assertEquals(0, requiredNumberOfBitsForEncoding(value, value));
		}

		assertEquals(63, requiredNumberOfBitsForEncoding(0, Long.MAX_VALUE));
		assertEquals(64, requiredNumberOfBitsForEncoding(-1, Long.MAX_VALUE));
		assertEquals(64, requiredNumberOfBitsForEncoding(Long.MIN_VALUE, Long.MAX_VALUE));

		assertEquals(63, requiredNumberOfBitsForEncoding(Long.MIN_VALUE, -1));
		assertEquals(64, requiredNumberOfBitsForEncoding(Long.MIN_VALUE, 0));
	}

	private String repeat(char c, int amount) {
		char[] chars = new char[amount];
		Arrays.fill(chars, c);
		return new String(chars);
	}

	private String zeros(int amount) {
		return repeat('0', amount);
	}

	private String ones(int amount) {
		return repeat('1', amount);
	}

	@Test
	public void testEncodeUniformIntegerFullLongRange() {
		test(zeros(62) + "10", output -> encodeUniformInteger(2, Long.MIN_VALUE, Long.MAX_VALUE, output));
		test("0" + ones(63), output -> encodeUniformInteger(Long.MAX_VALUE, -1, Long.MAX_VALUE, output));
		test(ones(64), output -> encodeUniformInteger(-1, Long.MIN_VALUE, 10L, output));
		test("1" + zeros(63), output -> encodeUniformInteger(Long.MIN_VALUE, Long.MIN_VALUE, 100, output));
	}

	@Test
	public void testEncodeUniformIntegerPartial() {
		test("100", output -> encodeUniformInteger(4, 0, 4, output));
		test("100", output -> encodeUniformInteger(4, 0, 7, output));
		test("100", output -> encodeUniformInteger(-100, -104, -100, output));
		test("100", output -> encodeUniformInteger(-100, -104, -97, output));

		test("011", output -> encodeUniformInteger(3, 0, 4, output));
		test("011", output -> encodeUniformInteger(3, 0, 7, output));
		test("11", output -> encodeUniformInteger(3, 0, 3, output));
		test("11", output -> encodeUniformInteger(4, 1, 4, output));

		test(ones(63), output -> encodeUniformInteger(Long.MAX_VALUE, 0, Long.MAX_VALUE, output));
		test(zeros(63), output -> encodeUniformInteger(0, 0, Long.MAX_VALUE, output));
		test(ones(63), output -> encodeUniformInteger(-1, Long.MIN_VALUE, -1, output));
		test(zeros(63), output -> encodeUniformInteger(Long.MIN_VALUE, Long.MIN_VALUE, -1, output));

		test("", output -> encodeUniformInteger(10, 10, 10, output));
	}

	@Test
	public void testEncodePositiveVariableInteger() {
		test("0 0", output -> encodeVariableInteger(0, 0, 100, output));
		test("1 0", output -> encodeVariableInteger(1, 0, 100, output));
		test("0 1 010 0", output -> encodeVariableInteger(2, 0, 100, output));
		test("0 1 011 0", output -> encodeVariableInteger(3, 0, 100, output));
		test("0 1 100 0", output -> encodeVariableInteger(4, 0, 100, output));
		test("0 1 101 0", output -> encodeVariableInteger(5, 0, 100, output));
		test("0 1 110 0", output -> encodeVariableInteger(6, 0, 100, output));
		test("0 1 111 0", output -> encodeVariableInteger(7, 0, 100, output));
		test("1 1 000 0", output -> encodeVariableInteger(8, 0, 100, output));
		test("1 1 001 0", output -> encodeVariableInteger(9, 0, 100, output));
		test("1 1 010 0", output -> encodeVariableInteger(10, 0, 100, output));
		test("1 1 111 0", output -> encodeVariableInteger(15, 0, 100, output));
		test("0 1 010 1 000", output -> encodeVariableInteger(16, 0, 100, output));
		test("0 1 100 1 000", output -> encodeVariableInteger(32, 0, 100, output));
		test("1 1 000 1 000", output -> encodeVariableInteger(64, 0, 100, output));

		test("0 0", output -> encodeVariableInteger(0, 0, 10, output));
		test("1 0", output -> encodeVariableInteger(1, 0, 10, output));
		test("0 1 010", output -> encodeVariableInteger(2, 0, 10, output));
		test("0 1 011", output -> encodeVariableInteger(3, 0, 10, output));
		test("0 1 100", output -> encodeVariableInteger(4, 0, 10, output));
		test("0 1 101", output -> encodeVariableInteger(5, 0, 10, output));
		test("0 1 110", output -> encodeVariableInteger(6, 0, 10, output));
		test("0 1 111", output -> encodeVariableInteger(7, 0, 10, output));
		test("1 1 000", output -> encodeVariableInteger(8, 0, 10, output));
		test("1 1 001", output -> encodeVariableInteger(9, 0, 10, output));
		test("1 1 010", output -> encodeVariableInteger(10, 0, 10, output));

		test("0 0", output -> encodeVariableInteger(100, 100, 200, output));
		test("1 0", output -> encodeVariableInteger(101, 100, 200, output));
		test("1 1 010", output -> encodeVariableInteger(110, 100, 110, output));

		test("0 0", output -> encodeVariableInteger(0, 0, Long.MAX_VALUE, output));
		test(ones(63 + 5), output -> encodeVariableInteger(Long.MAX_VALUE, 0, Long.MAX_VALUE, output));
		test(ones(63 + 5 - 1) + "0", output -> encodeVariableInteger(Long.MAX_VALUE - 1, 0, Long.MAX_VALUE, output));
	}

	@Test
	public void testEncodeNegativeVariableInteger() {
		test("0 0", output -> encodeVariableInteger(0, -100, 0, output));
		test("1 0", output -> encodeVariableInteger(-1, -100, 0, output));
		test("0 1 010 0", output -> encodeVariableInteger(-2, -100, 0, output));
		test("0 1 011 0", output -> encodeVariableInteger(-3, -100, 0, output));
		test("0 1 100 0", output -> encodeVariableInteger(-4, -100, 0, output));
		test("0 1 101 0", output -> encodeVariableInteger(-5, -100, 0, output));
		test("0 1 110 0", output -> encodeVariableInteger(-6, -100, 0, output));
		test("0 1 111 0", output -> encodeVariableInteger(-7, -100, 0, output));
		test("1 1 000 0", output -> encodeVariableInteger(-8, -100, 0, output));
		test("1 1 001 0", output -> encodeVariableInteger(-9, -100, 0, output));
		test("1 1 010 0", output -> encodeVariableInteger(-10, -100, 0, output));
		test("0 1 010 1 000", output -> encodeVariableInteger(-16, -100, 0, output));
		test("0 1 100 1 000", output -> encodeVariableInteger(-32, -100, 0, output));
		test("1 1 000 1 000", output -> encodeVariableInteger(-64, -100, 0, output));

		test("0 0", output -> encodeVariableInteger(0, -10, 0, output));
		test("1 0", output -> encodeVariableInteger(-1, -10, 0, output));
		test("0 1 010", output -> encodeVariableInteger(-2, -10, 0, output));
		test("0 1 011", output -> encodeVariableInteger(-3, -10, 0, output));
		test("0 1 100", output -> encodeVariableInteger(-4, -10, 0, output));
		test("0 1 101", output -> encodeVariableInteger(-5, -10, 0, output));
		test("0 1 110", output -> encodeVariableInteger(-6, -10, 0, output));
		test("0 1 111", output -> encodeVariableInteger(-7, -10, 0, output));
		test("1 1 000", output -> encodeVariableInteger(-8, -10, 0, output));
		test("1 1 001", output -> encodeVariableInteger(-9, -10, 0, output));
		test("1 1 010", output -> encodeVariableInteger(-10, -10, 0, output));

		test("0 0", output -> encodeVariableInteger(-100, -200, -100, output));
		test("1 0", output -> encodeVariableInteger(-101, -200, -100, output));
		test("1 1 010", output -> encodeVariableInteger(-110, -110, -100, output));

		test("0 0", output -> encodeVariableInteger(0, Long.MIN_VALUE, 0, output));
		test(ones(63 + 5) + "1", output -> encodeVariableInteger(Long.MIN_VALUE, Long.MIN_VALUE, 0, output));
		test(ones(63 + 5) + "0", output -> encodeVariableInteger(Long.MIN_VALUE + 1, Long.MIN_VALUE, 0, output));
		test(ones(63 + 5 - 1) + "0", output -> encodeVariableInteger(Long.MIN_VALUE + 2, Long.MIN_VALUE, 0, output));
		test(ones(63 + 5 - 2) + "01", output -> encodeVariableInteger(Long.MIN_VALUE + 3, Long.MIN_VALUE, 0, output));
	}

	@Test
	public void testEncodeVariableIntegerAroundZero() {
		test("0 00", output -> encodeVariableInteger(0, -10, 10, output));
		test("1 00", output -> encodeVariableInteger(1, -10, 10, output));
		test("0 01", output -> encodeVariableInteger(-1, -10, 10, output));
		test("1 01", output -> encodeVariableInteger(-2, -10, 10, output));
		test("0 00", output -> encodeVariableInteger(0, Long.MIN_VALUE, Long.MAX_VALUE, output));
		test("1 00", output -> encodeVariableInteger(1, Long.MIN_VALUE, Long.MAX_VALUE, output));
		test("0 01", output -> encodeVariableInteger(-1, Long.MIN_VALUE, Long.MAX_VALUE, output));
		test("1 01", output -> encodeVariableInteger(-2, Long.MIN_VALUE, Long.MAX_VALUE, output));
		test("0 00", output -> encodeVariableInteger(0, Long.MIN_VALUE, Long.MAX_VALUE, output));
		test("1 00", output -> encodeVariableInteger(1, -1, Long.MAX_VALUE, output));
		test("0 01", output -> encodeVariableInteger(-1, -10, Long.MAX_VALUE, output));
		test("1 01", output -> encodeVariableInteger(-2, Long.MIN_VALUE, 1, output));

		test("0 1 010 0", output -> encodeVariableInteger(2, -10, 10, output));
		test("0 1 011 0", output -> encodeVariableInteger(3, -10, 10, output));
		test("0 1 100 0", output -> encodeVariableInteger(4, -10, 10, output));
		test("0 1 101 0", output -> encodeVariableInteger(5, -1, 10, output));
		test("0 1 110 0", output -> encodeVariableInteger(6, -10, 10, output));
		test("0 1 111 0", output -> encodeVariableInteger(7, -10, 10, output));
		test("1 1 000 0", output -> encodeVariableInteger(8, -10, 10, output));
		test("1 1 001 0", output -> encodeVariableInteger(9, -10, 10, output));
		test("1 1 010 0", output -> encodeVariableInteger(10, -1, 10, output));

		test("0 1 010 1", output -> encodeVariableInteger(-3, -10, 10, output));
		test("0 1 011 1", output -> encodeVariableInteger(-4, -10, 10, output));
		test("0 1 100 1", output -> encodeVariableInteger(-5, -10, 1, output));
		test("0 1 101 1", output -> encodeVariableInteger(-6, -10, 10, output));
		test("0 1 110 1", output -> encodeVariableInteger(-7, -10, 10, output));
		test("0 1 111 1", output -> encodeVariableInteger(-8, -10, 10, output));
		test("1 1 000 1", output -> encodeVariableInteger(-9, -10, 10, output));
		test("1 1 001 1", output -> encodeVariableInteger(-10, -10, 1, output));
	}

	private void test(String expected, ThrowingConsumer<BitOutputStream> encoder) {
		BitStringStream bitStream = new BitStringStream();
		try {
			encoder.accept(bitStream);
		} catch (Throwable e) {
			throw new AssertionError(e);
		}

		assertEquals(expected.replaceAll(" ", ""), bitStream.get());
	}

	@Test
	public void testDecode() throws IOException {
		for (long value = 0; value <= 100; value++) {
			test(value, 0, 100);
			test(value, 0, 10_000);
			test(value, 0, Long.MAX_VALUE);
		}
		for (long value = -100; value <= 0; value++) {
			test(value, -100, 0);
			test(value, -10_000, 0);
			test(value, Long.MIN_VALUE, 0);
		}
		for (long value = -100; value <= 100; value++) {
			test(value, -100, 100);
			test(value, -10_000, 10_000);
			test(value, Long.MIN_VALUE, Long.MAX_VALUE);
		}
		for (long value = -8000; value <= -7000; value++) {
			test(value, -8000, -7000);
			test(value, -100_000, -7000);
			test(value, -100_000, -2000);
			test(value, Long.MIN_VALUE, Long.MAX_VALUE);
		}
		for (long value = 7000; value <= 8000; value++) {
			test(value, 7000, 8000);
			test(value, 7000, 100_000);
			test(value, 2000, 100_000);
			test(value, Long.MIN_VALUE, Long.MAX_VALUE);
		}

		for (long minValue : new long[]{-100, 0}) {
			for (long value = 0; value <= 100_000; value++) test(value, minValue, Long.MAX_VALUE);
			test(Long.MAX_VALUE, minValue, Long.MAX_VALUE);
		}

		for (long maxValue : new long[]{0, 100}) {
			for (long value = -100_000; value <= 0; value++) test(value, Long.MIN_VALUE, maxValue);
			test(Long.MIN_VALUE, Long.MIN_VALUE, maxValue);
		}

		for (long candidate : new long[]{
				Long.MIN_VALUE, Long.MIN_VALUE + 1, Long.MIN_VALUE + 2, -100, -1, 0,
				Long.MAX_VALUE, Long.MAX_VALUE - 1, Long.MAX_VALUE - 2, 100, 1
		}) {
			test(candidate, Long.MIN_VALUE, Long.MAX_VALUE);
			if (candidate != Long.MIN_VALUE) test(candidate, Long.MIN_VALUE + 1, Long.MAX_VALUE);
			if (candidate < 0) {
				test(candidate, Long.MIN_VALUE, 0);
				if (candidate != Long.MIN_VALUE) test(candidate, Long.MIN_VALUE + 1, 0);
			}
			if (candidate > 0) {
				test(candidate, 0, Long.MAX_VALUE);
				if (candidate != Long.MAX_VALUE) test(candidate, 0, Long.MAX_VALUE - 1);
			}
		}
	}

	private void test(long value, long minValue, long maxValue) throws IOException {
		ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
		BitOutputStream bitOutput = new BitOutputStream(byteOutput);
		encodeVariableInteger(value, minValue, maxValue, bitOutput);
		encodeUniformInteger(1234, 0, 10_000, bitOutput);
		bitOutput.finish();

		BitInputStream bitInput = new BitInputStream(new ByteArrayInputStream(byteOutput.toByteArray()));
		assertEquals(value, decodeVariableInteger(minValue, maxValue, bitInput));
		assertEquals(1234, decodeUniformInteger(0, 10_000, bitInput));
		bitInput.close();
	}
}
