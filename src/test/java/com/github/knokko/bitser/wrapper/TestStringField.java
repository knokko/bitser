package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.field.StringField;
import com.github.knokko.bitser.serialize.Bitser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@BitStruct(backwardCompatible = false)
public class TestStringField {

	@BitField(optional = true)
	public String a;

	@StringField(length = @IntegerField(minValue = 5, maxValue = 10, expectUniform = true))
	public String b;

	@Test
	public void testOptionalStrings() {
		Bitser bitser = new Bitser(false);
		assertThrows(InvalidBitValueException.class, () -> bitser.serializeToBytes(this));
		this.b = "abcde";

		TestStringField loaded = bitser.deepCopy(this);
		assertNull(loaded.a);
		assertEquals("abcde", loaded.b);
	}

	@Test
	public void testUnicodeStrings() {
		this.a = "Ώ ΐ Α Β Γ Δ Ε Ζ Η Θ Ι ";
		this.b = " ນ ບ ";

		TestStringField loaded = new Bitser(false).deepCopy(this);
		assertEquals(this.a, loaded.a);
		assertEquals(this.b, loaded.b);
	}
}
