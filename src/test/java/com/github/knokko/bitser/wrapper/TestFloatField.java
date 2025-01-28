package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.FloatField;
import com.github.knokko.bitser.serialize.Bitser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@BitStruct(backwardCompatible = true)
public class TestFloatField {

	@BitField(optional = true, id = 10)
	@FloatField
	public Float optional;

	@BitField(id = 20)
	@FloatField(expectMultipleOf = 0.2)
	public double required;

	@Test
	public void testInvalidConfigurations() {
		assertThrows(InvalidBitFieldException.class, () -> new Bitser(true).deepCopy(new Invalid1()));
		assertThrows(InvalidBitFieldException.class, () -> new Bitser(true).deepCopy(new Invalid2()));
	}

	@Test
	public void testValidConfiguration() {
		Bitser bitser = new Bitser(false);
		this.optional = null;
		this.required = 1.8;

		TestFloatField loaded = bitser.deepCopy(this);
		assertNull(loaded.optional);
		assertEquals(1.8, loaded.required);

		this.optional = Float.NaN;
		this.required = -123.456;
		loaded = bitser.deepCopy(this);
		assertTrue(Float.isNaN(loaded.optional));
		assertEquals(this.required, loaded.required);
	}

	@Test
	public void testExpectMultipleOf() {
		Bitser bitser = new Bitser(false);

		this.optional = null;
		this.required = 1.4;

		byte[] bytes = bitser.serializeToBytes(this);

		// This should fit in 2 bytes
		assertEquals(2, bytes.length);

		TestFloatField loaded = bitser.deserializeFromBytes(TestFloatField.class, bytes);
		assertNull(loaded.optional);
		assertEquals(1.4, loaded.required, 0.001);
	}

	@BitStruct(backwardCompatible = false)
	private static class Invalid1 {

		@BitField(optional = true)
		@FloatField
		@SuppressWarnings("unused")
		public float invalid;
	}

	@BitStruct(backwardCompatible = false)
	private static class Invalid2 {

		@FloatField
		@SuppressWarnings("unused")
		public int invalid;
	}
}
