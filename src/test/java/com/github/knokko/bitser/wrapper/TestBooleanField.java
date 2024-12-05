package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.io.BitserHelper;
import com.github.knokko.bitser.serialize.Bitser;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@BitStruct(backwardCompatible = false)
public class TestBooleanField {

	@BitField(ordering = 0, optional = true)
	Boolean optional;

	@BitField(ordering = 1)
	boolean required;

	@Test
	public void testValid() throws IOException {
		this.optional = null;
		this.required = true;

		TestBooleanField loaded = BitserHelper.serializeAndDeserialize(new Bitser(false), this);
		assertNull(loaded.optional);
		assertTrue(loaded.required);

		this.optional = false;
		this.required = false;

		loaded = BitserHelper.serializeAndDeserialize(new Bitser(false), this);
		assertFalse(loaded.optional);
		assertFalse(loaded.required);
	}

	@BitStruct(backwardCompatible = false)
	private static class Invalid {

		@SuppressWarnings("unused")
		@BitField(ordering = 0, optional = true)
		boolean invalid;
	}

	@Test
	public void testInvalidOptional() {
		InvalidBitFieldException invalid = assertThrows(InvalidBitFieldException.class,
				() -> new Bitser(true).serialize(new Invalid(), new BitOutputStream(new ByteArrayOutputStream()))
		);
		assertTrue(
				invalid.getMessage().contains("can't be optional"),
				"Expected " + invalid.getMessage() + " to contain \"can't be optional\""
		);
	}
}
