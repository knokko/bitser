package com.github.knokko.bitser.backward;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.backward.instance.LegacyStructInstance;
import com.github.knokko.bitser.backward.instance.LegacyValues;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.field.*;
import com.github.knokko.bitser.serialize.BitPostInit;
import com.github.knokko.bitser.serialize.WithParameter;
import com.github.knokko.bitser.serialize.Bitser;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.github.knokko.bitser.wrapper.TestHelper.assertContains;
import static org.junit.jupiter.api.Assertions.*;

public class TestReferenceBackwardCompatibility {

	@BitStruct(backwardCompatible = true)
	private static class Dummy {

		@BitField(id = 2)
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
	private static class NewDummy implements BitPostInit {

		@BitField(id = 2)
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
	private static class ReferenceMethodNew implements BitPostInit {

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

	@BitStruct(backwardCompatible = true)
	private static class ReferenceMethodNewCorrupted implements BitPostInit {

		@BitField(id = 0)
		@ReferenceFieldTarget(label = "dummy")
		final ArrayList<NewDummy> dummies = new ArrayList<>();

		@BitField(id = 1)
		NewDummy best;

		@Override
		public void postInit(Context context) {
			if (best != null) return;
			LegacyStructInstance legacyDummy = (LegacyStructInstance) context.legacyFunctionValues.get(ReferenceMethodNewCorrupted.class)[0];
			LegacyValues legacyValues = legacyDummy.valuesHierarchy.get(0);
			assertArrayEquals(new boolean[] { false, false, true }, legacyValues.hadValues);
			int x = (int) (long) legacyValues.values[2];
			//noinspection SuspiciousNameCombination
			best = new NewDummy(x, x);
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

	@Test
	public void testReferenceMethodCorrupted() {
		Bitser bitser = new Bitser(true);
		ReferenceMethodOldCorrupted before = new ReferenceMethodOldCorrupted();
		before.dummies.add(new Dummy(5));

		ReferenceMethodNewCorrupted after = bitser.deserializeFromBytes(ReferenceMethodNewCorrupted.class, bitser.serializeToBytes(
				before, Bitser.BACKWARD_COMPATIBLE, new WithParameter("best", 0)
		), Bitser.BACKWARD_COMPATIBLE);
		assertEquals(1, after.dummies.size());
		assertEquals(5, after.best.x);
		assertEquals(5, after.best.y);
		assertNotSame(after.best, after.dummies.get(0));
		assertEquals(5, after.dummies.get(0).x);
		assertEquals(5, after.dummies.get(0).y);

		after.dummies.add(new NewDummy(123, 456));
		ReferenceMethodNewCorrupted again = bitser.deepCopy(after, Bitser.BACKWARD_COMPATIBLE);
		assertEquals(2, again.dummies.size());
		assertEquals(5, again.best.x);
		assertEquals(5, again.best.y);
		assertNotSame(after.best, again.dummies.get(0));
		assertEquals(5, again.dummies.get(0).x);
		assertEquals(5, again.dummies.get(0).y);
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
	private static class NewNestedRoot implements BitPostInit {

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

	@BitStruct(backwardCompatible = true)
	private static class SimpleUnstable {

		@BitField(id = 0)
		@ReferenceFieldTarget(label = "the one")
		Dummy target;

		@BitField(id = 1)
		@ReferenceField(stable = false, label = "the one")
		Dummy reference;
	}

	@BitStruct(backwardCompatible = true)
	private static class SimpleStable {

		@BitField(id = 0)
		@ReferenceFieldTarget(label = "the one")
		StableDummy target;

		@BitField(id = 1)
		@ReferenceField(stable = true, label = "the one")
		StableDummy reference;
	}

	@Test
	public void testUnstableToStableReference() {
		Bitser bitser = new Bitser(true);
		SimpleUnstable unstable = new SimpleUnstable();
		unstable.target = new Dummy(75);
		unstable.reference = unstable.target;

		SimpleStable stable = bitser.deserializeFromBytes(
				SimpleStable.class, bitser.serializeToBytes(unstable, Bitser.BACKWARD_COMPATIBLE), Bitser.BACKWARD_COMPATIBLE
		);
		assertEquals(-1.0, stable.target.rating);
		assertSame(stable.target, stable.reference);
	}

	@BitStruct(backwardCompatible = true)
	private static class SimpleUnstable2 {

		@BitField(id = 0)
		@ReferenceFieldTarget(label = "the one")
		StableDummy target;

		@BitField(id = 1)
		@ReferenceField(stable = false, label = "the one")
		StableDummy reference;
	}

	@Test
	public void testStableToUnstableReference() {
		Bitser bitser = new Bitser(true);
		SimpleStable stable = new SimpleStable();
		stable.target = new StableDummy(75.75);
		stable.reference = stable.target;

		SimpleUnstable2 loaded = bitser.deserializeFromBytes(
				SimpleUnstable2.class, bitser.serializeToBytes(stable, Bitser.BACKWARD_COMPATIBLE), Bitser.BACKWARD_COMPATIBLE
		);
		assertEquals(75.75, loaded.target.rating);
		assertSame(loaded.target, loaded.reference);
	}

	@BitStruct(backwardCompatible = true)
	private static class SimpleNonReference {

		@BitField(id = 0)
		Dummy target;

		@BitField(id = 1)
		Dummy reference;
	}

	@Test
	public void testReferenceToNonReference() {
		Bitser bitser = new Bitser(false);
		SimpleUnstable unstable = new SimpleUnstable();
		unstable.target = new Dummy(45);
		unstable.reference = unstable.target;

		String errorMessage = assertThrows(InvalidBitValueException.class, () -> bitser.deserializeFromBytes(
				SimpleNonReference.class, bitser.serializeToBytes(unstable, Bitser.BACKWARD_COMPATIBLE), Bitser.BACKWARD_COMPATIBLE
		)).getMessage();
		assertContains(errorMessage, "SimpleNonReference.reference");
	}

	@Test
	public void testNonReferenceToReference() {
		Bitser bitser = new Bitser(true);
		SimpleNonReference simple = new SimpleNonReference();
		simple.reference = new Dummy(1);
		simple.target = new Dummy(2);

		String errorMessage = assertThrows(InvalidBitValueException.class, () -> bitser.deserializeFromBytes(
				SimpleUnstable.class, bitser.serializeToBytes(simple, Bitser.BACKWARD_COMPATIBLE), Bitser.BACKWARD_COMPATIBLE
		)).getMessage();
		assertContains(errorMessage, "SimpleUnstable.reference");
	}

	@BitStruct(backwardCompatible = true)
	private static class NonReferenceFunction {

		@BitField(id = 0)
		Dummy target;

		@SuppressWarnings("unused")
		@BitField(id = 1)
		Dummy reference() {
			return new Dummy(65);
		}
	}

	@BitStruct(backwardCompatible = true)
	private static class ReferenceFunction {

		@BitField(id = 0)
		@ReferenceFieldTarget(label = "test")
		Dummy target;

		@SuppressWarnings("unused")
		@BitField(id = 1)
		@ReferenceField(stable = false, label = "test")
		Dummy reference() {
			return target;
		}
	}

	@Test
	public void testReferenceFunctionToNonReferenceFunction() {
		Bitser bitser = new Bitser(false);
		ReferenceFunction unstable = new ReferenceFunction();
		unstable.target = new Dummy(45);

		String errorMessage = assertThrows(InvalidBitValueException.class, () -> bitser.deserializeFromBytes(
				NonReferenceFunction.class, bitser.serializeToBytes(unstable, Bitser.BACKWARD_COMPATIBLE), Bitser.BACKWARD_COMPATIBLE
		)).getMessage();
		assertContains(errorMessage, "NonReferenceFunction.reference");
	}

	@Test
	public void testNonReferenceFunctionToReferenceFunction() {
		Bitser bitser = new Bitser(true);
		NonReferenceFunction simple = new NonReferenceFunction();
		simple.target = new Dummy(2);

		String errorMessage = assertThrows(InvalidBitValueException.class, () -> bitser.deserializeFromBytes(
				ReferenceFunction.class, bitser.serializeToBytes(simple, Bitser.BACKWARD_COMPATIBLE), Bitser.BACKWARD_COMPATIBLE
		)).getMessage();
		assertContains(errorMessage, "ReferenceFunction.reference");
	}

	@BitStruct(backwardCompatible = true)
	private static class OptionalUnstable {

		@BitField(id = 0)
		@ReferenceFieldTarget(label = "the one")
		Dummy target;

		@BitField(id = 1, optional = true)
		@ReferenceField(stable = false, label = "the one")
		Dummy reference;
	}

	@Test
	public void testNonReferenceToOptionalReference() {
		Bitser bitser = new Bitser(true);
		SimpleNonReference simple = new SimpleNonReference();
		simple.reference = new Dummy(1);
		simple.target = new Dummy(2);

		OptionalUnstable unstable = bitser.deserializeFromBytes(OptionalUnstable.class, bitser.serializeToBytes(
				simple, Bitser.BACKWARD_COMPATIBLE
		), Bitser.BACKWARD_COMPATIBLE);
		assertEquals(2, unstable.target.x);
		assertNull(unstable.reference);
	}

	@BitStruct(backwardCompatible = true)
	private static class OptionalNonReference {

		@BitField(id = 0)
		Dummy target;

		@BitField(id = 1, optional = true)
		Dummy reference;
	}

	@Test
	public void testReferenceToOptionalNonReference() {
		Bitser bitser = new Bitser(true);
		SimpleUnstable unstable = new SimpleUnstable();
		unstable.target = new Dummy(50);
		unstable.reference = unstable.target;

		OptionalNonReference simple = bitser.deserializeFromBytes(OptionalNonReference.class, bitser.serializeToBytes(
				unstable, Bitser.BACKWARD_COMPATIBLE
		), Bitser.BACKWARD_COMPATIBLE);
		assertEquals(50, simple.target.x);
		assertNull(simple.reference);
	}

	@Test
	public void testBackwardCompatibleWith() {
		Bitser bitser = new Bitser(false);
		OldNestedRoot oldWith = new OldNestedRoot();
		oldWith.stableRoot = new StableDummy(1.0);
		oldWith.friendRoot = new Dummy(2);
		oldWith.targets = new OldTargetWrapper[] { new OldTargetWrapper(new StableDummy(3), oldWith.friendRoot) };
		oldWith.friends.add(new FriendWrapper(new Dummy(4), oldWith.stableRoot));

		OldNestedRoot oldSubject = new OldNestedRoot();
		oldSubject.stableRoot = new StableDummy(5.0);
		oldSubject.friendRoot = new Dummy(6);
		oldSubject.friends.add(new FriendWrapper(new Dummy(7), oldWith.targets[0].dummy));
		oldSubject.targets = new OldTargetWrapper[] { new OldTargetWrapper(new StableDummy(8), oldWith.friendRoot) };

		byte[] oldSubjectBytes = bitser.serializeToBytes(oldSubject, oldWith, Bitser.BACKWARD_COMPATIBLE);
		byte[] oldWithBytes = bitser.serializeToBytes(oldWith, Bitser.BACKWARD_COMPATIBLE);

		NewNestedRoot newWith = bitser.deserializeFromBytes(NewNestedRoot.class, oldWithBytes, Bitser.BACKWARD_COMPATIBLE);
		assertEquals(1.0, newWith.stableRoot.rating);
		assertEquals(2, newWith.friendRoot.x);
		assertEquals(3.0, newWith.targets.get(0).dummy.rating);
		assertSame(newWith.friendRoot, newWith.targets.get(0).friend);
		assertEquals(4, newWith.friends.iterator().next().friend.x);
		assertSame(newWith.stableRoot, newWith.friends.iterator().next().cross);

		NewNestedRoot newSubject = bitser.deserializeFromBytes(NewNestedRoot.class, oldSubjectBytes, Bitser.BACKWARD_COMPATIBLE, newWith);
		assertEquals(5.0, newSubject.stableRoot.rating);
		assertEquals(6, newSubject.friendRoot.x);
		assertEquals(7, newSubject.friends.iterator().next().friend.x);
		assertEquals(8.0, newSubject.targets.get(0).dummy.rating);
		assertSame(newWith.targets.get(0).dummy, newSubject.friends.iterator().next().cross);
		assertSame(newWith.friendRoot, newSubject.targets.get(0).friend);

		NewNestedRoot back = bitser.deserializeFromBytes(NewNestedRoot.class, bitser.serializeToBytes(
				newSubject, Bitser.BACKWARD_COMPATIBLE, newWith
		), newWith, Bitser.BACKWARD_COMPATIBLE);
		assertSame(newWith.targets.get(0).dummy, back.friends.iterator().next().cross);
		assertSame(newWith.friendRoot, back.targets.get(0).friend);
	}

	@BitStruct(backwardCompatible = true)
	private static class LotsOfReferences {

		@SuppressWarnings("unused")
		@ReferenceField(stable = false, label = "zones")
		private static final boolean ZONE_KEY_PROPERTIES = false;

		@SuppressWarnings("unused")
		@ReferenceField(stable = false, label = "attacks")
		private static final boolean ATTACK_VALUE_PROPERTIES = false;

		@BitField(id = 0)
		@ReferenceFieldTarget(label = "attacks")
		final ArrayList<String> attacks = new ArrayList<>();

		@BitField(id = 1)
		@ReferenceFieldTarget(label = "structs")
		final ArrayList<String> structs = new ArrayList<>();

		@BitField(id = 2)
		@ReferenceFieldTarget(label = "zones")
		final ArrayList<String> zones = new ArrayList<>();

		@BitField(id = 3)
		@NestedFieldSetting(path = "k", fieldName = "ZONE_KEY_PROPERTIES")
		@NestedFieldSetting(path = "v", fieldName = "ATTACK_VALUE_PROPERTIES")
		final HashMap<String, String> bestAttacksPerZone = new HashMap<>();

		@BitField(id = 4)
		@ReferenceField(stable = false, label = "structs")
		String bestStruct;
	}

	@Test
	public void testNoBuiltinLabelConflicts() {
		LotsOfReferences lots = new LotsOfReferences();
		for (int counter = 0; counter < 10; counter++) lots.attacks.add("Attack " + counter);
		for (int counter = 0; counter < 100; counter++) lots.structs.add("Struct " + counter);
		for (int counter = 0; counter < 1000; counter++) lots.zones.add("Zone " + counter);
		lots.bestAttacksPerZone.put(lots.zones.get(385), lots.attacks.get(3));
		lots.bestStruct = lots.structs.get(72);

		LotsOfReferences copy = new Bitser(false).deepCopy(lots, Bitser.BACKWARD_COMPATIBLE);
		assertEquals(lots.attacks, copy.attacks);
		assertNotSame(lots.attacks, copy.attacks);
		assertEquals(lots.structs, copy.structs);
		assertEquals(lots.zones, copy.zones);
		assertSame(copy.structs.get(72), copy.bestStruct);
		assertEquals(1, copy.bestAttacksPerZone.size());
		assertSame(copy.attacks.get(3), copy.bestAttacksPerZone.get(copy.zones.get(385)));
	}

	@BitStruct(backwardCompatible = true)
	private static class ReferenceWrapper1 {

		@BitField(id = 0)
		@ReferenceField(stable = false, label = "a")
		final String string;

		ReferenceWrapper1(String string) {
			this.string = string;
		}

		@SuppressWarnings("unused")
		ReferenceWrapper1() {
			this(null);
		}
	}

	@BitStruct(backwardCompatible = true)
	private static class ReferenceWrapper2 {

		@BitField(id = 0)
		@ReferenceField(stable = false, label = "b")
		final String string;

		ReferenceWrapper2(String string) {
			this.string = string;
		}

		@SuppressWarnings("unused")
		ReferenceWrapper2() {
			this(null);
		}
	}

	@BitStruct(backwardCompatible = true)
	private static class ReferenceTarget1 {

		@BitField(id = 0)
		@ReferenceFieldTarget(label = "a")
		final String string;

		ReferenceTarget1(String string) {
			this.string = string;
		}

		@SuppressWarnings("unused")
		ReferenceTarget1() {
			this(null);
		}
	}

	@BitStruct(backwardCompatible = true)
	private static class ReferenceTarget2 {

		@BitField(id = 0)
		@ReferenceFieldTarget(label = "b")
		final String string;

		ReferenceTarget2(String string) {
			this.string = string;
		}

		@SuppressWarnings("unused")
		ReferenceTarget2() {
			this(null);
		}
	}

	@BitStruct(backwardCompatible = true)
	private static class MapOfStructs {

		@BitField(id = 0)
		final HashMap<String, ReferenceTarget1> target1 = new HashMap<>();

		@BitField(id = 1)
		final HashMap<ReferenceTarget2, String> target2 = new HashMap<>();

		@BitField(id = 2)
		final HashMap<String, ReferenceWrapper1> reference1 = new HashMap<>();

		@BitField(id = 3)
		final HashMap<ReferenceWrapper2, String> reference2 = new HashMap<>();
	}

	@Test
	public void testMapOfStructs() {
		MapOfStructs root = new MapOfStructs();
		String target1 = "ok";
		String target2 = "welcome";
		root.target1.put("hello", new ReferenceTarget1(target1));
		root.target2.put(new ReferenceTarget2(target2), "nice");
		root.reference1.put("pretty", new ReferenceWrapper1(target1));
		root.reference2.put(new ReferenceWrapper2(target2), "taste");

		MapOfStructs copy = new Bitser(true).deepCopy(root, Bitser.BACKWARD_COMPATIBLE);
		assertEquals(1, copy.target1.size());
		assertEquals(1, copy.target2.size());
		assertEquals(1, copy.reference1.size());
		assertEquals(1, copy.reference2.size());
	}
}
