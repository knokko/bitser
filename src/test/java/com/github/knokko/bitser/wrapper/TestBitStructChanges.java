package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitEnum;
import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.FloatField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.Bitser;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TestBitStructChanges {

	@BitStruct(backwardCompatible = false)
	private static class Primitives {

		@BitField(ordering = 0)
		boolean b1;

		@BitField(ordering = 1)
		boolean b2;

		@BitField(ordering = 2)
		@IntegerField(expectUniform = false)
		int i;

		@BitField(ordering = 3)
		@IntegerField(expectUniform = false)
		int j;

		@BitField(ordering = 4)
		@FloatField
		float f;

		@BitField(ordering = 5)
		@FloatField
		double d;
	}

	private byte[] captureChanges(Bitser bitser, Object original, Object modified, int expectedAmount) throws IOException {
		// TODO Maybe create method in Bitser for this
		ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
		BitOutputStream bitOutput = new BitOutputStream(byteOutput);
		int numChanges = bitser.cache.getWrapper(original.getClass()).findAndWriteChanges(bitser, bitOutput, original, modified);
		assertEquals(expectedAmount, numChanges);
		bitOutput.finish();
		return byteOutput.toByteArray();
	}

	private void applyChanges(Bitser bitser, Object target, byte[] changes) throws IOException {
		BitInputStream bitInput = new BitInputStream(new ByteArrayInputStream(changes));
		bitser.cache.getWrapper(target.getClass()).readAndApplyChanges(bitser, bitInput, target);
	}

	@Test
	public void testFindPrimitiveChanges() throws IOException {
		Bitser bitser = new Bitser(false);

		Primitives original = new Primitives();
		original.j = 12;
		original.f = 1f;

		Primitives modified = bitser.cache.getWrapper(original.getClass()).shallowCopy(original);
		modified.b1 = true;
		modified.i = 1234;
		modified.j = 12; // Note that it was already 12
		modified.f = 3f;

		byte[] changes = captureChanges(bitser, original, modified, 3);

		original.d = 60f;

		applyChanges(bitser, original, changes);

		assertTrue(original.b1);
		assertFalse(original.b2);
		assertEquals(1234, original.i);
		assertEquals(12, original.j);
		assertEquals(3f, original.f);
		assertEquals(60f, original.d);
	}

	@BitStruct(backwardCompatible = false)
	private static class PrimitiveWrappers {

		@BitField(ordering = 0)
		Boolean b1;

		@BitField(ordering = 1, optional = true)
		Boolean b2;

		@BitField(ordering = 2)
		@IntegerField(expectUniform = false)
		Integer i;

		@BitField(ordering = 3, optional = true)
		@IntegerField(expectUniform = false)
		Integer j;

		@BitField(ordering = 4)
		@FloatField
		Float f;

		@BitField(ordering = 5, optional = true)
		@FloatField
		Double d;
	}

	@Test
	public void testFindPrimitiveWrapperChanges() throws IOException {
		Bitser bitser = new Bitser(false);
		PrimitiveWrappers original = new PrimitiveWrappers();
		original.b1 = true;
		original.i = 1234;
		original.f = -1234.5f;

		PrimitiveWrappers notReallyModified = bitser.cache.getWrapper(original.getClass()).shallowCopy(original);
		notReallyModified.b1 = true;
		notReallyModified.i = 1234;
		assertNotSame(original.i, notReallyModified.i);
		notReallyModified.f = -1234.5f;
		assertNotSame(original.f, notReallyModified.f);

		captureChanges(bitser, original, notReallyModified, 0);

		PrimitiveWrappers actuallyModified = bitser.cache.getWrapper(original.getClass()).shallowCopy(notReallyModified);
		actuallyModified.b2 = false;
		actuallyModified.i = 1235;
		actuallyModified.d = 1234.5;

		byte[] changes = captureChanges(bitser, original, actuallyModified, 3);
		applyChanges(bitser, original, changes);

		assertTrue(original.b1);
		assertFalse(original.b2);
		assertEquals(1235, original.i);
		assertNull(original.j);
		assertEquals(-1234.5f, original.f);
		assertEquals(1234.5, original.d);
	}

	@BitEnum(mode = BitEnum.Mode.UniformOrdinal)
	private enum ExampleEnum {
		A,
		B,
		C
	}

	@BitStruct(backwardCompatible = false)
	private static class NoNesting {

		@BitField(ordering = 0, optional = true)
		String s1;

		@BitField(ordering = 1)
		String s2;

		@BitField(ordering = 2, optional = true)
		UUID id1;

		@BitField(ordering = 3)
		UUID id2;

		@BitField(ordering = 4, optional = true)
		ExampleEnum example1;

		@BitField(ordering = 5)
		ExampleEnum example2;
	}

	@Test
	public void testFindSimpleNonNestedChanges() throws IOException {
		NoNesting original = new NoNesting();
		original.s1 = "hello";
		original.s2 = "hi";
		original.id1 = new UUID(1, 2);
		original.id2 = new UUID(3, 4);
		original.example1 = ExampleEnum.A;
		original.example2 = ExampleEnum.C;

		NoNesting modified = new NoNesting();
		modified.s2 = "world";
		modified.id2 = original.id2;
		modified.example2 = ExampleEnum.B;

		Bitser bitser = new Bitser(false);
		byte[] changes = captureChanges(bitser, original, modified, 5);
		applyChanges(bitser, original, changes);

		assertNull(original.s1);
		assertEquals("world", original.s2);
		assertNull(original.id1);
		assertEquals(modified.id2, original.id2);
		assertNull(original.example1);
		assertEquals(ExampleEnum.B, original.example2);
	}
}
