package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.io.BitCountStream;
import com.github.knokko.bitser.serialize.Bitser;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.github.knokko.bitser.wrapper.TestHelper.assertContains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@BitStruct(backwardCompatible = false)
public class TestIntegerField {

	@IntegerField(expectUniform = false, minValue = 10, maxValue = 100_000)
	private int varInt;

	@IntegerField(expectUniform = true, maxValue = 100)
	private int uniformInt;

	@Test
	public void testZero() {
		this.varInt = 10;
		TestIntegerField loaded = new Bitser(false).deepCopy(this);
		assertEquals(10, loaded.varInt);
		assertEquals(0, loaded.uniformInt);
	}

	@Test
	public void testNonZero() {
		this.varInt = 12345;
		this.uniformInt = -123456;
		TestIntegerField loaded = new Bitser(true).deepCopy(this);
		assertEquals(12345, loaded.varInt);
		assertEquals(-123456, loaded.uniformInt);
	}

	@Test
	public void testOutOfRange() {
		this.varInt = 9;
		String errorMessage = assertThrows(InvalidBitValueException.class,
				() -> new Bitser(true).deepCopy(this)
		).getMessage();
		assertContains(errorMessage, "9 is out of range [10, 100000]");
		assertContains(errorMessage, " -> varInt");
	}

	@BitStruct(backwardCompatible = false)
	private static class SingleShort {

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
