package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.io.BitserHelper;
import com.github.knokko.bitser.serialize.Bitser;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
