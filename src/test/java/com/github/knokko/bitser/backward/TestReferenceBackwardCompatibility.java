package com.github.knokko.bitser.backward;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.field.ReferenceField;
import com.github.knokko.bitser.field.ReferenceFieldTarget;
import com.github.knokko.bitser.serialize.Bitser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class TestReferenceBackwardCompatibility {

	@BitStruct(backwardCompatible = true)
	private static class Dummy {

		@BitField(id = 0)
		@IntegerField(expectUniform = false)
		final int x;

		Dummy(int x) {
			this.x = x;
		}

		Dummy() {
			this(0);
		}
	}

	@BitStruct(backwardCompatible = true)
	private static class VerySimpleShallow {

		@BitField(id = 0)
		@IntegerField(expectUniform = false)
		int a;

		@BitField(id = 1)
		@ReferenceFieldTarget(label = "dummy")
		Dummy[] targets;

		@BitField(id = 2)
		@ReferenceField(stable = false, label = "dummy")
		Dummy reference;

		@BitField(id = 3)
		@IntegerField(expectUniform = false)
		int b;
	}

	@Test
	public void testVerySimpleShallow() {
		Bitser bitser = new Bitser(true);
		VerySimpleShallow original = new VerySimpleShallow();
		original.a = 1;
		original.targets = new Dummy[2];
		original.targets[0] = new Dummy(5);
		original.targets[1] = new Dummy(6);
		original.reference = original.targets[0];
		original.b = 9;

		VerySimpleShallow loaded = bitser.deepCopy(original, Bitser.BACKWARD_COMPATIBLE);
		assertEquals(1, loaded.a);
		assertEquals(2, loaded.targets.length);
		assertEquals(5, loaded.targets[0].x);
		assertEquals(6, loaded.targets[1].x);
		assertSame(loaded.targets[0], loaded.reference);
		assertEquals(9, loaded.b);
	}
}
