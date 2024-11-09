package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.io.BitserHelper;
import com.github.knokko.bitser.serialize.Bitser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@BitStruct(backwardCompatible = false)
public class TestUUIDFieldWrapper {

	@BitField(ordering = 0, optional = true)
	private UUID optional;

	@BitField(ordering = 1)
	private UUID required;

	@Test
	public void testNullAndZero() throws IOException {
		this.required = new UUID(0, 0);
		TestUUIDFieldWrapper loaded = BitserHelper.serializeAndDeserialize(new Bitser(true), this);
		assertNull(loaded.optional);
		assertEquals(new UUID(0, 0), loaded.required);
	}

	@Test
	public void testGeneral() throws IOException {
		this.optional = UUID.randomUUID();
		this.required = new UUID(12, 345);
		TestUUIDFieldWrapper loaded = BitserHelper.serializeAndDeserialize(new Bitser(false), this);
		assertEquals(this.optional, loaded.optional);
		assertEquals(new UUID(12, 345), loaded.required);
	}
}
