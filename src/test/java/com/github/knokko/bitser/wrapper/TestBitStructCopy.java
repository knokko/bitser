package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.field.*;
import com.github.knokko.bitser.serialize.Bitser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestBitStructCopy {

	@BitStruct(backwardCompatible = false)
	private static class ChildStruct {

		@BitField(ordering = 0)
		@IntegerField(expectUniform = false)
		final int x;

		@SuppressWarnings("unused")
		ChildStruct() {
			this(0);
		}

		ChildStruct(int x) {
			this.x = x;
		}
	}

	@BitStruct(backwardCompatible = false)
	private static class Root {

		@BitField(ordering = 0)
		boolean b;

		@BitField(ordering = 1)
		ChildStruct c = new ChildStruct(15);

		@BitField(ordering = 2)
		@FloatField
		double d = 1.25;

		@BitField(ordering = 3)
		@IntegerField(expectUniform = false)
		int i = 12;

		@BitField(ordering = 4)
		@IntegerField(expectUniform = false)
		long[] l = { 1 };

		@BitField(ordering = 5)
		@ReferenceFieldTarget(label = "children")
		ChildStruct target = new ChildStruct(20);

		@BitField(ordering = 6)
		@ReferenceField(stable = false, label = "children")
		ChildStruct reference = target;
	}

	@Test
	public void testShallowCopy() {
		Root root = new Root();
		root.b = true;
		root.c = new ChildStruct(16);
		root.d = -5.5;
		root.i = 321;
		root.l = new long[] { 12, -34 };
		root.target = new ChildStruct(-123);
		root.reference = root.target;

		Root copied = new Bitser(false).cache.getWrapper(root.getClass()).shallowCopy(root);
		assertNotSame(root, copied);
		assertTrue(copied.b);
		assertEquals(16, copied.c.x);
		assertSame(root.c, copied.c);
		assertEquals(-5.5, copied.d);
		assertEquals(321, copied.i);
		assertSame(root.l, copied.l);
		assertSame(root.target, copied.target);
		assertSame(copied.reference, copied.target);
	}
}
