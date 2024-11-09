package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.io.BitCountStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.io.BitserHelper;
import com.github.knokko.bitser.serialize.Bitser;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@BitStruct(backwardCompatible = false)
public class TestIntegerFieldWrapper {

	@BitField(ordering = 0)
	@IntegerField(expectUniform = false, minValue = 10, maxValue = 100_000)
	private int varInt;

	@BitField(ordering = 1)
	@IntegerField(expectUniform = true, maxValue = 100)
	private int uniformInt;

	@Test
	public void testZero() throws IOException {
		this.varInt = 10;
		TestIntegerFieldWrapper loaded = BitserHelper.serializeAndDeserialize(new Bitser(false), this);
		assertEquals(10, loaded.varInt);
		assertEquals(0, loaded.uniformInt);
	}

	@Test
	public void testNonZero() throws IOException {
		this.varInt = 12345;
		this.uniformInt = -123456;
		TestIntegerFieldWrapper loaded = BitserHelper.serializeAndDeserialize(new Bitser(true), this);
		assertEquals(12345, loaded.varInt);
		assertEquals(-123456, loaded.uniformInt);
	}

	@Test
	public void testOutOfRange() {
		this.varInt = 9;
		InvalidBitValueException failed = assertThrows(InvalidBitValueException.class,
				() -> new Bitser(true).serialize(this, new BitOutputStream(new ByteArrayOutputStream()))
		);
		assertEquals("9 is out of range [10, 100000] for " +
				"private int com.github.knokko.bitser.wrapper.TestIntegerFieldWrapper.varInt", failed.getMessage());
	}

	@BitStruct(backwardCompatible = false)
	private static class SingleShort {

		@BitField(ordering = 0)
		@IntegerField(expectUniform = true)
		Short value;
	}

	@Test
	public void testNumberOfShortBits() throws IOException {
		SingleShort instance = new SingleShort();
		instance.value = 123;

		Bitser bitser = new Bitser(true);
		BitCountStream counter = new BitCountStream();
		bitser.serialize(instance, counter);

		assertEquals(16, counter.getCounter());
	}
}
