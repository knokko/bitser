package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.field.StringField;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.io.BitserHelper;
import com.github.knokko.bitser.serialize.Bitser;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@BitStruct(backwardCompatible = false)
public class TestStringField {

	@BitField(ordering = 1, optional = true)
	@StringField
	public String a;

	@BitField(ordering = 0)
	@StringField(length = @IntegerField(minValue = 5, maxValue = 10, expectUniform = true))
	public String b;

	@Test
	public void testOptionalStrings() throws IOException {
		Bitser bitser = new Bitser(false);
		assertThrows(NullPointerException.class, () -> bitser.serialize(this, new BitOutputStream(new ByteArrayOutputStream())));
		this.b = "abcde";

		TestStringField loaded = BitserHelper.serializeAndDeserialize(bitser, this);
		assertNull(loaded.a);
		assertEquals("abcde", loaded.b);
	}

	@Test
	public void testUnicodeStrings() throws IOException {
		this.a = "Ώ ΐ Α Β Γ Δ Ε Ζ Η Θ Ι ";
		this.b = " ນ ບ ";

		TestStringField loaded = BitserHelper.serializeAndDeserialize(new Bitser(false), this);
		assertEquals(this.a, loaded.a);
		assertEquals(this.b, loaded.b);
	}
}
