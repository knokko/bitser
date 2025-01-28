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
	private static class Before {

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
	private static class After {

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
		Before before = new Before();
		before.dummyChance = 12;
		before.dummyFraction = 2.5f;
		before.byeBye = "Bye bye";

		byte[] bytes1 = bitser.serializeToBytes(before, Bitser.BACKWARD_COMPATIBLE);
		After after = bitser.deserializeFromBytes(After.class, bytes1, Bitser.BACKWARD_COMPATIBLE);
		assertEquals(12, after.weirdChance);
		assertEquals(2.5f, after.dummyFraction);
		assertNotNull(after.newID);

		byte[] bytes2 = bitser.serializeToBytes(after, Bitser.BACKWARD_COMPATIBLE);
		Before back = bitser.deserializeFromBytes(Before.class, bytes2, Bitser.BACKWARD_COMPATIBLE);
		assertEquals(12, back.dummyChance);
		assertEquals(2.5f, back.dummyFraction);
		assertNull(back.byeBye);
	}

	// TODO Test saving some backward-compatible structs in a non-backward-compatible way
	// TODO Handle struct fields
}
