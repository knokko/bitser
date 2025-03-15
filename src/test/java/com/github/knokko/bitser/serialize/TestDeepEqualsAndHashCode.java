package com.github.knokko.bitser.serialize;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.field.ReferenceField;
import com.github.knokko.bitser.field.ReferenceFieldTarget;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestDeepEqualsAndHashCode {

	@BitStruct(backwardCompatible = false)
	private static class Point {

		@IntegerField(expectUniform = false)
		int x;

		@SuppressWarnings("unused")
		@IntegerField(expectUniform = false)
		int y;

		int z;
	}

	@Test
	public void testSimpleEqualsAndHashCode() {
		Bitser bitser = new Bitser(false);
		Point p = new Point();
		p.x = 5;
		Point q = new Point();

		assertFalse(bitser.deepEquals(p, q));
		assertNotEquals(bitser.hashCode(p), bitser.hashCode(q));

		q.x = 5;
		assertTrue(bitser.deepEquals(p, q));
		assertEquals(bitser.hashCode(p), bitser.hashCode(q));

		p.z = 10; // z is not a BitField, so irrelevant
		assertTrue(bitser.deepEquals(p, q));
		assertEquals(bitser.hashCode(p), bitser.hashCode(q));
	}

	@BitStruct(backwardCompatible = true)
	private static class Face {

		@BitField(id = 1)
		boolean isSmiling;
	}

	@BitStruct(backwardCompatible = true)
	private static class Friend {

		@BitField(id = 0)
		@ReferenceFieldTarget(label = "faces")
		final Face face = new Face();
	}

	@BitStruct(backwardCompatible = true)
	private static class ComplexStruct {

		@BitField(id = 5)
		final Face myFace = new Face();

		@BitField(id = 0)
		@ReferenceField(stable = false, label = "faces")
		Face friendlyFace;
	}

	@Test
	public void testComplexDeepEqualsAndHashCode() {
		Bitser bitser = new Bitser(true);

		Friend friend1 = new Friend();
		friend1.face.isSmiling = true;
		Friend friend2 = new Friend();
		friend2.face.isSmiling = true;

		ComplexStruct a = new ComplexStruct();
		ComplexStruct b = new ComplexStruct();
		a.friendlyFace = friend1.face;
		b.friendlyFace = friend2.face;

		assertFalse(bitser.deepEquals(a, b, friend1, friend2));
		assertNotEquals(bitser.hashCode(a, friend1, friend2), bitser.hashCode(b, friend1, friend2));

		a.friendlyFace = friend2.face;
		assertTrue(bitser.deepEquals(a, b, friend1, friend2, Bitser.BACKWARD_COMPATIBLE));
		assertEquals(bitser.hashCode(a, friend2), bitser.hashCode(b, friend2));

		a.myFace.isSmiling = true;
		assertFalse(bitser.deepEquals(a, b, friend1, friend2, Bitser.BACKWARD_COMPATIBLE));
		assertNotEquals(bitser.hashCode(a, friend2), bitser.hashCode(b, friend2));
		assertNotEquals(
				bitser.hashCode(a, friend2, Bitser.BACKWARD_COMPATIBLE),
				bitser.hashCode(b, friend2, Bitser.BACKWARD_COMPATIBLE)
		);
	}
}
