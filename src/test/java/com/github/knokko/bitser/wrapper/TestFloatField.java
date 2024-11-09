package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.FloatField;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.io.BitserHelper;
import com.github.knokko.bitser.serialize.Bitser;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@BitStruct(backwardCompatible = false)
public class TestFloatField {

	@BitField(ordering = 0, optional = true)
	@FloatField
	public Float optional;

	@BitField(ordering = 1)
	@FloatField(expectMultipleOf = 0.2)
	public double required;

	@Test
	public void testInvalidConfigurations() {
		assertThrows(InvalidBitFieldException.class, () -> new Bitser(true).serialize(
				new Invalid1(), new BitOutputStream(new ByteArrayOutputStream())
		));
		assertThrows(InvalidBitFieldException.class, () -> new Bitser(true).serialize(
				new Invalid2(), new BitOutputStream(new ByteArrayOutputStream())
		));
	}

	@Test
	public void testValidConfiguration() throws IOException {
		this.optional = null;
		this.required = 1.8;

		TestFloatField loaded = BitserHelper.serializeAndDeserialize(new Bitser(false), this);
		assertNull(loaded.optional);
		assertEquals(1.8, loaded.required);

		this.optional = Float.NaN;
		this.required = -123.456;
		loaded = BitserHelper.serializeAndDeserialize(new Bitser(true), this);
		assertTrue(Float.isNaN(loaded.optional));
		assertEquals(this.required, loaded.required);
	}

	@Test
	public void testExpectMultipleOf() throws IOException {
		Bitser bitser = new Bitser(false);

		ByteArrayOutputStream byteOutput = new ByteArrayOutputStream(1);
		BitOutputStream bitOutput = new BitOutputStream(byteOutput);

		this.optional = null;
		this.required = 1.4;

		bitser.serialize(this, bitOutput);
		bitOutput.finish();

		// This should fit in 2 bytes
		assertEquals(2, byteOutput.toByteArray().length);

		TestFloatField loaded = bitser.deserialize(
				TestFloatField.class, new BitInputStream(new ByteArrayInputStream(byteOutput.toByteArray()))
		);
		assertNull(loaded.optional);
		assertEquals(1.4, loaded.required, 0.001);
	}

	@BitStruct(backwardCompatible = false)
	private static class Invalid1 {

		@BitField(ordering = 0, optional = true)
		@FloatField
		@SuppressWarnings("unused")
		public float invalid;
	}

	@BitStruct(backwardCompatible = false)
	private static class Invalid2 {

		@BitField(ordering = 0)
		@FloatField
		@SuppressWarnings("unused")
		public int invalid;
	}
}
