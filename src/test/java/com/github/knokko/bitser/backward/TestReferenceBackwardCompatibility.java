package com.github.knokko.bitser.backward;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.field.*;
import com.github.knokko.bitser.init.PostInit;
import com.github.knokko.bitser.init.WithParameter;
import com.github.knokko.bitser.serialize.Bitser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.UUID;

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

	@BitStruct(backwardCompatible = true)
	private static class StableDummy {

		@SuppressWarnings("unused")
		@BitField(id = 0)
		@StableReferenceFieldId
		final UUID id = UUID.randomUUID();

		@BitField(id = 1)
		@FloatField(expectMultipleOf = 0.1)
		final double rating;

		StableDummy(double rating) {
			this.rating = rating;
		}

		@SuppressWarnings("unused")
		StableDummy() {
			this(-1.0);
		}
	}

	@BitStruct(backwardCompatible = true)
	private static class OldMixedReferenceList {

		@BitField(id = 0)
		@ReferenceFieldTarget(label = "dummies")
		final ArrayList<StableDummy> targets = new ArrayList<>();

		@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
		@BitField(id = 1)
		@ReferenceField(stable = true, label = "dummies")
		final ArrayList<StableDummy> stableReferences = new ArrayList<>();

		@BitField(id = 2)
		@ReferenceField(stable = false, label = "dummies")
		final ArrayList<ArrayList<StableDummy>> unstableReferences = new ArrayList<>();
	}

	@BitStruct(backwardCompatible = true)
	private static class NewMixedReferenceList {

		@BitField(id = 0)
		@ReferenceFieldTarget(label = "dummies")
		final StableDummy[] targets;

		@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
		@BitField(id = 1)
		@ReferenceField(stable = true, label = "dummies")
		final LinkedList<StableDummy> stableReferences = new LinkedList<>();

		@BitField(id = 2)
		@ReferenceField(stable = false, label = "dummies")
		StableDummy[][] unstableReferences;

		@SuppressWarnings("unused")
		NewMixedReferenceList(StableDummy[] targets) {
			this.targets = targets;
		}

		@SuppressWarnings("unused")
		NewMixedReferenceList() {
			this.targets = null;
		}
	}

	@Test
	public void testStableReferences() {
		Bitser bitser = new Bitser(true);
		OldMixedReferenceList before = new OldMixedReferenceList();
		before.targets.add(new StableDummy(0.5));
		before.targets.add(new StableDummy(-2.5));
		before.stableReferences.add(before.targets.get(1));
		before.stableReferences.add(before.targets.get(0));
		before.stableReferences.add(before.targets.get(1));
		before.unstableReferences.add(new ArrayList<>(1));
		before.unstableReferences.get(0).add(before.targets.get(1));

		NewMixedReferenceList after = bitser.deserializeFromBytes(NewMixedReferenceList.class, bitser.serializeToBytes(
				before, Bitser.BACKWARD_COMPATIBLE
		), Bitser.BACKWARD_COMPATIBLE);
		assert after.targets != null;
		assertEquals(2, after.targets.length);
		assertEquals(0.5, after.targets[0].rating);
		assertEquals(-2.5, after.targets[1].rating);
		assertEquals(3, after.stableReferences.size());
		assertSame(after.targets[1], after.stableReferences.get(0));
		assertSame(after.targets[0], after.stableReferences.get(1));
		assertSame(after.targets[1], after.stableReferences.get(0));
		assertEquals(1, after.unstableReferences.length);
		assertEquals(1, after.unstableReferences[0].length);
		assertSame(after.targets[1], after.unstableReferences[0][0]);
	}

	@BitStruct(backwardCompatible = true)
	private static class OldTargetWrapper {

		@BitField(id = 0)
		@ReferenceFieldTarget(label = "dummies")
		final StableDummy dummy;

		@BitField(id = 2)
		@ReferenceField(stable = false, label = "friends")
		final Dummy friend;

		OldTargetWrapper(StableDummy dummy, Dummy friend) {
			this.dummy = dummy;
			this.friend = friend;
		}

		@SuppressWarnings("unused")
		OldTargetWrapper() {
			this(null, null);
		}
	}

	@BitStruct(backwardCompatible = true)
	private static class NewTargetWrapper {

		@BitField(id = 0)
		@ReferenceFieldTarget(label = "dummies")
		final StableDummy dummy;

		@BitField(id = 1)
		@IntegerField(expectUniform = false)
		final int x;

		@BitField(id = 2)
		@ReferenceField(stable = false, label = "friends")
		final Dummy friend;

		NewTargetWrapper(StableDummy dummy, int x, Dummy friend) {
			this.dummy = dummy;
			this.x = x;
			this.friend = friend;
		}

		@SuppressWarnings("unused")
		NewTargetWrapper() {
			this(null, 123, null);
		}
	}

	@BitStruct(backwardCompatible = true)
	private static class FriendWrapper {

		@BitField(id = 5)
		@ReferenceFieldTarget(label = "friends")
		final Dummy friend;

		@BitField(id = 2)
		@ReferenceField(stable = true, label = "dummies")
		final StableDummy cross;

		FriendWrapper(Dummy friend, StableDummy cross) {
			this.friend = friend;
			this.cross = cross;
		}

		@SuppressWarnings("unused")
		FriendWrapper() {
			this(null, null);
		}
	}

	@BitStruct(backwardCompatible = true)
	private static class OldNestedRoot {

		@BitField(id = 4)
		@ReferenceFieldTarget(label = "dummies")
		StableDummy stableRoot;

		@BitField(id = 3)
		@ReferenceFieldTarget(label = "friends")
		Dummy friendRoot;

		@BitField(id = 2)
		OldTargetWrapper[] targets;

		@BitField(id = 1)
		final ArrayList<FriendWrapper> friends = new ArrayList<>();
	}

	@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
	@BitStruct(backwardCompatible = true)
	private static class NewNestedRoot implements PostInit {

		@BitField(id = 4)
		@ReferenceFieldTarget(label = "dummies")
		StableDummy stableRoot;

		@BitField(id = 3)
		@ReferenceFieldTarget(label = "friends")
		Dummy friendRoot;

		@BitField(id = 2)
		final LinkedList<NewTargetWrapper> targets = new LinkedList<>();

		@BitField(id = 1)
		final HashSet<FriendWrapper> friends = new HashSet<>();

		int x;

		@Override
		public void postInit(Context context) {
			this.x = targets.size() + friends.size();
		}
	}

	@Test
	public void testNested() {
		Bitser bitser = new Bitser(true);
		OldNestedRoot oldRoot = new OldNestedRoot();
		oldRoot.stableRoot = new StableDummy(4.5);
		oldRoot.friendRoot = new Dummy(12);
		oldRoot.targets = new OldTargetWrapper[] {
				new OldTargetWrapper(new StableDummy(17), oldRoot.friendRoot),
				null
		};
		oldRoot.friends.add(new FriendWrapper(new Dummy(31), oldRoot.targets[0].dummy));
		oldRoot.targets[1] = new OldTargetWrapper(new StableDummy(99), oldRoot.friends.get(0).friend);

		NewNestedRoot newRoot = bitser.deserializeFromBytes(NewNestedRoot.class, bitser.serializeToBytes(
				oldRoot, Bitser.BACKWARD_COMPATIBLE
		), Bitser.BACKWARD_COMPATIBLE);
		assertEquals(4.5, newRoot.stableRoot.rating);
		assertEquals(12, newRoot.friendRoot.x);
		assertEquals(2, newRoot.targets.size());
		assertEquals(17, newRoot.targets.get(0).dummy.rating);
		assertEquals(123, newRoot.targets.get(0).x);
		assertSame(newRoot.friendRoot, newRoot.targets.get(0).friend);
		assertEquals(99, newRoot.targets.get(1).dummy.rating);
		assertEquals(123, newRoot.targets.get(1).x);
		assertSame(newRoot.friends.iterator().next().friend, newRoot.targets.get(1).friend);
		assertEquals(1, newRoot.friends.size());
		assertEquals(31, newRoot.friends.iterator().next().friend.x);
		assertSame(newRoot.targets.get(0).dummy, newRoot.friends.iterator().next().cross);
		assertEquals(3, newRoot.x);
	}
}
