package com.github.knokko.bitser.backward;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.FloatField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.field.StableReferenceFieldId;
import com.github.knokko.bitser.serialize.Bitser;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TestSimpleBackwardCompatibility {

	@BitStruct(backwardCompatible = true)
	private static class SimpleBefore {

		@BitField(id = 0)
		@IntegerField(expectUniform = true, minValue = 0, maxValue = 100)
		int dummyChance;

		@BitField(id = 1)
		@FloatField(expectMultipleOf = 0.5)
		float dummyFraction;

		@BitField(id = 5)
		String byeBye;
	}

	@BitStruct(backwardCompatible = true)
	private static class SimpleAfter {

		@BitField(id = 0)
		@IntegerField(expectUniform = false, minValue = -1)
		int weirdChance;

		@BitField(id = 1)
		@FloatField(expectMultipleOf = 0.01)
		float dummyFraction;

		@BitField(id = 10)
		@StableReferenceFieldId
		final UUID newID = UUID.randomUUID();
	}

	@Test
	public void testSimpleBackwardCompatibility() {
		Bitser bitser = new Bitser(false);
		SimpleBefore before = new SimpleBefore();
		before.dummyChance = 12;
		before.dummyFraction = 2.5f;
		before.byeBye = "Bye bye";

		byte[] bytes1 = bitser.serializeToBytes(before, Bitser.BACKWARD_COMPATIBLE);
		SimpleAfter after = bitser.deserializeFromBytes(SimpleAfter.class, bytes1, Bitser.BACKWARD_COMPATIBLE);
		assertEquals(12, after.weirdChance);
		assertEquals(2.5f, after.dummyFraction);
		assertNotNull(after.newID);

		byte[] bytes2 = bitser.serializeToBytes(after, Bitser.BACKWARD_COMPATIBLE);
		SimpleBefore back = bitser.deserializeFromBytes(SimpleBefore.class, bytes2, Bitser.BACKWARD_COMPATIBLE);
		assertEquals(12, back.dummyChance);
		assertEquals(2.5f, back.dummyFraction);
		assertNull(back.byeBye);
	}

	// TODO Test saving some backward-compatible structs in a non-backward-compatible way
	// TODO Handle struct fields

	@BitStruct(backwardCompatible = true)
	private static class NestedBefore {

		@BitField(id = 2)
		final SimpleBefore nested = new SimpleBefore();

		@BitField(id = 1)
		@IntegerField(expectUniform = true)
		int test = 45;

		// TODO Add field with id 3
	}

	@BitStruct(backwardCompatible = true)
	private static class NestedAfter {

		@BitField(id = 2)
		final SimpleAfter nested = new SimpleAfter();

		@BitField(id = 1)
		@IntegerField(expectUniform = true)
		byte test = 22;
	}

	@Test
	public void testNested() {
		Bitser bitser = new Bitser(true);

		NestedBefore before = new NestedBefore();
		before.nested.dummyChance = 99;
		before.nested.dummyFraction = 0.025f;
		before.nested.byeBye = "Hey";
		before.test += 1;

		byte[] bytes1 = bitser.serializeToBytes(before, Bitser.BACKWARD_COMPATIBLE);
		NestedAfter after = bitser.deserializeFromBytes(NestedAfter.class, bytes1, Bitser.BACKWARD_COMPATIBLE);
		assertEquals(99, after.nested.weirdChance);
		assertEquals(0.025f, after.nested.dummyFraction);
		assertEquals(23, after.test);

		byte[] bytes2 = bitser.serializeToBytes(after, Bitser.BACKWARD_COMPATIBLE);
		NestedBefore back = bitser.deserializeFromBytes(NestedBefore.class, bytes2, Bitser.BACKWARD_COMPATIBLE);
		assertEquals(99, back.nested.dummyChance);
		assertEquals(0.025f, back.nested.dummyFraction);
		assertEquals(23, back.test);
	}
}
