package com.github.knokko.bitser.backward;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.field.*;
import com.github.knokko.bitser.init.PostInit;
import com.github.knokko.bitser.init.WithParameter;
import com.github.knokko.bitser.serialize.Bitser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class TestReferenceBackwardCompatibility {

	@BitStruct(backwardCompatible = true)
	private static class Dummy {

		@BitField(id = 0)
		@IntegerField(expectUniform = false)
		final int x;

		Dummy(int x) {
			this.x = x;
		}

		@SuppressWarnings("unused")
		Dummy() {
			this(0);
		}
	}

	@BitStruct(backwardCompatible = true)
	private static class NewDummy implements PostInit { // TODO Move PostInit class

		@BitField(id = 0)
		@IntegerField(expectUniform = false)
		final int x;

		@BitField(id = 1)
		@IntegerField(expectUniform = false)
		int y;

		NewDummy(int x, int y) {
			this.x = x;
			this.y = y;
		}

		@SuppressWarnings("unused")
		NewDummy() {
			this(0, 0);
		}

		@Override
		public void postInit(Context context) {
			Object[] legacyValues = context.legacyFieldValues.get(NewDummy.class);
			if (legacyValues.length <= 1 || legacyValues[1] == null) {//noinspection SuspiciousNameCombination
				this.y = this.x;
			}
		}
	}

	@BitStruct(backwardCompatible = true)
	private static class OldShallow {

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

		@BitField(id = 4)
		@ReferenceFieldTarget(label = "text")
		String[] stringTargets;

		@BitField(id = 5)
		@ReferenceField(stable = false, label = "text")
		String stringReference;
	}

	@BitStruct(backwardCompatible = true)
	private static class NewShallow {

		@BitField(id = 0)
		@IntegerField(expectUniform = false)
		int a;

		@BitField(id = 1)
		@ReferenceFieldTarget(label = "dummy")
		NewDummy[] targets;

		@BitField(id = 2)
		@ReferenceField(stable = false, label = "dummy")
		NewDummy reference;

		@BitField(id = 3)
		@IntegerField(expectUniform = false)
		int b;

		@BitField(id = 4)
		@ReferenceFieldTarget(label = "text")
		String[] stringTargets;

		@BitField(id = 5)
		@ReferenceField(stable = false, label = "text")
		String stringReference;
	}

	@Test
	public void testVerySimpleShallow() {
		Bitser bitser = new Bitser(true);
		OldShallow original = new OldShallow();
		original.a = 1;
		original.targets = new Dummy[2];
		original.targets[0] = new Dummy(5);
		original.targets[1] = new Dummy(6);
		original.reference = original.targets[0];
		original.b = 9;
		original.stringTargets = new String[] { "ok" };
		original.stringReference = original.stringTargets[0];

		OldShallow loaded = bitser.deepCopy(original, Bitser.BACKWARD_COMPATIBLE);
		assertEquals(1, loaded.a);
		assertEquals(2, loaded.targets.length);
		assertEquals(5, loaded.targets[0].x);
		assertEquals(6, loaded.targets[1].x);
		assertSame(loaded.targets[0], loaded.reference);
		assertEquals(9, loaded.b);
		assertArrayEquals(new String[] { "ok" }, loaded.stringTargets);
		assertSame(loaded.stringTargets[0], loaded.stringReference);

		NewShallow newer = bitser.deserializeFromBytes(NewShallow.class, bitser.serializeToBytes(
				original, Bitser.BACKWARD_COMPATIBLE
		), Bitser.BACKWARD_COMPATIBLE);
		assertEquals(1, newer.a);
		assertEquals(2, newer.targets.length);
		assertEquals(5, newer.targets[0].x);
		assertEquals(5, newer.targets[0].y);
		assertEquals(6, newer.targets[1].x);
		assertEquals(6, newer.targets[1].y);
		assertSame(newer.targets[0], newer.reference);
		assertEquals(9, newer.b);
		assertArrayEquals(new String[] { "ok" }, newer.stringTargets);
		assertSame(newer.stringTargets[0], newer.stringReference);
	}

	@BitStruct(backwardCompatible = true)
	private static class ReferenceMethodOld {

		@BitField(id = 0)
		@ReferenceFieldTarget(label = "dummy")
		final ArrayList<Dummy> dummies = new ArrayList<>();

		@SuppressWarnings("unused")
		@BitField(id = 0)
		@ReferenceField(stable = false, label = "dummy")
		Dummy best(FunctionContext context) {
			return dummies.get((int) context.withParameters.get("best"));
		}
	}

	@BitStruct(backwardCompatible = true)
	private static class ReferenceMethodOldCorrupted {

		@BitField(id = 0)
		@ReferenceFieldTarget(label = "dummy")
		final ArrayList<Dummy> dummies = new ArrayList<>();

		@SuppressWarnings("unused")
		@BitField(id = 0)
		Dummy best(FunctionContext context) {
			return dummies.get((int) context.withParameters.get("best"));
		}
	}

	@BitStruct(backwardCompatible = true)
	private static class ReferenceMethodNew implements PostInit {

		@BitField(id = 0)
		@ReferenceFieldTarget(label = "dummy")
		final ArrayList<NewDummy> dummies = new ArrayList<>();

		@BitField(id = 1)
		@ReferenceField(stable = false, label = "dummy")
		NewDummy best;

		@Override
		public void postInit(Context context) {
			if (best != null) return;
			best = (NewDummy) context.legacyFunctionValues.get(ReferenceMethodNew.class)[0];
		}
	}

	@Test
	public void testReferenceMethod() {
		Bitser bitser = new Bitser(true);
		ReferenceMethodOld before = new ReferenceMethodOld();
		before.dummies.add(new Dummy(5));

		ReferenceMethodNew after = bitser.deserializeFromBytes(ReferenceMethodNew.class, bitser.serializeToBytes(
				before, Bitser.BACKWARD_COMPATIBLE, new WithParameter("best", 0)
		), Bitser.BACKWARD_COMPATIBLE);
		assertEquals(1, after.dummies.size());
		assertEquals(5, after.best.x);
		assertEquals(5, after.best.y);
		assertSame(after.best, after.dummies.get(0));

		after.dummies.add(new NewDummy(123, 456));
		ReferenceMethodNew again = bitser.deepCopy(after, Bitser.BACKWARD_COMPATIBLE);
		assertEquals(2, again.dummies.size());
		assertEquals(5, again.best.x);
		assertEquals(5, again.best.y);
		assertSame(again.best, again.dummies.get(0));
		assertEquals(123, again.dummies.get(1).x);
		assertEquals(456, again.dummies.get(1).y);
	}

	// TODO Test nested
	// TODO Test stable
}
