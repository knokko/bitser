package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.serialize.Bitser;
import org.junit.jupiter.api.Test;

import static com.github.knokko.bitser.wrapper.TestHelper.assertContains;
import static org.junit.jupiter.api.Assertions.*;

@BitStruct(backwardCompatible = false)
public class TestBooleanField {

	@BitField(optional = true)
	Boolean optional;

	@BitField
	boolean required;

	@Test
	public void testValid() {
		this.optional = null;
		this.required = true;

		TestBooleanField loaded = new Bitser(false).deepCopy(this);
		assertNull(loaded.optional);
		assertTrue(loaded.required);

		this.optional = false;
		this.required = false;

		loaded = new Bitser(false).deepCopy(this);
		assertFalse(loaded.optional);
		assertFalse(loaded.required);
	}

	@BitStruct(backwardCompatible = false)
	private static class Invalid {

		@SuppressWarnings("unused")
		@BitField(optional = true)
		boolean invalid;
	}

	@Test
	public void testInvalidOptional() {
		String errorMessage = assertThrows(InvalidBitFieldException.class,
				() -> new Bitser(true).serializeToBytes(new Invalid())
		).getMessage();
		assertContains(errorMessage, "can't be optional");
	}
}
