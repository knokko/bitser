package com.github.knokko.bitser.test.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.field.StringField;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.Bitser;
import com.github.knokko.bitser.options.CollectionSizeLimit;
import com.github.knokko.bitser.IntegerBitser;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static com.github.knokko.bitser.test.wrapper.TestHelper.assertContains;
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

	@Test
	public void testLargeMemoryAllocationAttack() throws IOException {
		Bitser bitser = new Bitser(true);

		ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
		BitOutputStream bitOutput = new BitOutputStream(byteOutput);
		bitOutput.write(true);
		IntegerBitser.encodeVariableInteger(Integer.MAX_VALUE, 0L, Integer.MAX_VALUE, bitOutput);
		bitOutput.finish();

		String errorMessage = assertThrows(InvalidBitValueException.class, () -> bitser.deserializeFromBytes(
				TestStringField.class, byteOutput.toByteArray(), new CollectionSizeLimit(1234)
		)).getMessage();
		assertContains(errorMessage, "length of 2147483647");
		assertContains(errorMessage, "exceeds the limit of");
		assertContains(errorMessage, "1234");
		assertContains(errorMessage, "-> string-value");
		assertContains(errorMessage, "-> a ->");
	}
}
