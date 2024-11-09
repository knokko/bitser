package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.CollectionField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.field.StringField;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.io.BitserHelper;
import com.github.knokko.bitser.serialize.Bitser;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

import static org.junit.jupiter.api.Assertions.*;

public class TestCollectionFieldWrapper {

	@BitStruct(backwardCompatible = false)
	private static class Strings {

		@StringField
		@SuppressWarnings("unused")
		private static final String STRINGS_VALUES = null;

		@BitField(ordering = 0)
		@CollectionField(optionalValues = true, valueAnnotations = "STRINGS_VALUES")
		private String[] array;

		@BitField(ordering = 1, optional = true)
		@CollectionField(valueAnnotations = "STRINGS_VALUES")
		private ArrayList<String> list;
	}

	@Test
	public void testNullStrings() throws IOException {
		Strings strings = new Strings();
		strings.array = new String[] { "hello", null, "world" };

		Strings loaded = BitserHelper.serializeAndDeserialize(new Bitser(false), strings);
		assertArrayEquals(new String[] { "hello", null, "world" }, loaded.array);
		assertNull(loaded.list);
	}

	@Test
	public void testNonNullStrings() throws IOException {
		Strings strings = new Strings();
		strings.array = new String[] { "test1234" };
		strings.list = new ArrayList<>();
		strings.list.add("hello, world!");

		Strings loaded = BitserHelper.serializeAndDeserialize(new Bitser(false), strings);
		assertArrayEquals(new String[] { "test1234" }, loaded.array);
		assertEquals(1, loaded.list.size());
		assertEquals("hello, world!", loaded.list.get(0));
	}

	@BitStruct(backwardCompatible = false)
	private static class Bytes {

		@SuppressWarnings("unused")
		@IntegerField(expectUniform = true)
		private static final byte PRIMITIVE_BYTES = 0;

		@SuppressWarnings("unused")
		@IntegerField(expectUniform = false)
		private static final Byte WRAPPED_BYTES = null;

		@BitField(ordering = 0)
		@CollectionField(
				valueAnnotations = "PRIMITIVE_BYTES",
				size = @IntegerField(minValue = 2, maxValue = 2, expectUniform = true)
		)
		private final byte[] array = new byte[2];

		@BitField(ordering = 1)
		@CollectionField(valueAnnotations = "WRAPPED_BYTES", optionalValues = true)
		private final LinkedList<Byte> list = new LinkedList<>();
	}

	@Test
	public void testNullAndZeroBytes() throws IOException {
		Bytes bytes = new Bytes();
		bytes.list.add((byte) 123);
		bytes.list.add(null);
		bytes.list.add((byte) 45);

		Bytes loaded = BitserHelper.serializeAndDeserialize(new Bitser(true), bytes);
		assertArrayEquals(new byte[2], loaded.array);
		assertEquals(3, loaded.list.size());
		assertEquals((byte) 123, loaded.list.get(0));
		assertNull(loaded.list.get(1));
		assertEquals((byte) 45, loaded.list.get(2));
	}

	@Test
	public void testNonNullBytes() throws IOException {
		Bytes bytes = new Bytes();
		bytes.array[0] = -12;
		bytes.array[1] = 34;
		bytes.list.add((byte) -123);

		Bytes loaded = BitserHelper.serializeAndDeserialize(new Bitser(false), bytes);
		assertArrayEquals(new byte[] { -12, 34 }, loaded.array);
		assertEquals(1, loaded.list.size());
		assertEquals((byte) -123, loaded.list.get(0));
	}

	@BitStruct(backwardCompatible = false)
	private static class InvalidShortArray {

		@SuppressWarnings("unused")
		@IntegerField(expectUniform = false)
		private static final short PRIMITIVE_SHORT = 0;

		@BitField(ordering = 0)
		@SuppressWarnings("unused")
		@CollectionField(optionalValues = true, valueAnnotations = "PRIMITIVE_SHORT")
		private short[] values;
	}

	@Test
	public void testInvalidShortArray() {
		InvalidShortArray invalid = new InvalidShortArray();
		InvalidBitFieldException failed = assertThrows(
				InvalidBitFieldException.class,
				() -> new Bitser(true).serialize(invalid, new BitOutputStream(new ByteArrayOutputStream()))
		);
		assertTrue(
				failed.getMessage().contains("can't be optional"),
				"Expected " + failed.getMessage() + " to contain \"can't be optional\""
		);
	}

	@BitStruct(backwardCompatible = false)
	private static class Longs {

		@SuppressWarnings("unused")
		@IntegerField(expectUniform = false)
		private static final Long VALUES = null;

		@BitField(ordering = 1, optional = true)
		@CollectionField(optionalValues = true, valueAnnotations = "VALUES")
		public Long[] array;

		@BitField(ordering = 0)
		@CollectionField(valueAnnotations = "VALUES")
		public HashSet<Long> set = new HashSet<>();
	}

	@Test
	public void testNullLongArray() throws IOException {
		Longs longs = new Longs();
		Longs loaded = BitserHelper.serializeAndDeserialize(new Bitser(true), longs);
		assertNull(loaded.array);
		assertEquals(0, loaded.set.size());
	}

	@Test
	public void testNullLongValues() throws IOException {
		Longs longs = new Longs();
		longs.array = new Long[] { 12L, null, 34L };
		Longs loaded = BitserHelper.serializeAndDeserialize(new Bitser(false), longs);
		assertArrayEquals(new Long[] { 12L, null, 34L }, loaded.array);
		assertEquals(0, loaded.set.size());
	}

	@Test
	public void testForbidLongSetNull() {
		Longs longs = new Longs();
		longs.set = null;
		NullPointerException failed = assertThrows(
				NullPointerException.class,
				() -> new Bitser(true).serialize(longs, new BitOutputStream(new ByteArrayOutputStream()))
		);
		assertTrue(
				failed.getMessage().contains("must not be null"),
				"Expected " + failed.getMessage() + " to contain \"must not be null\""
		);
	}

	@Test
	public void testForbidLongSetNullValues() {
		Longs longs = new Longs();
		longs.set.add(12L);
		longs.set.add(null);
		longs.set.add(34L);
		NullPointerException failed = assertThrows(
				NullPointerException.class,
				() -> new Bitser(true).serialize(longs, new BitOutputStream(new ByteArrayOutputStream()))
		);
		assertTrue(
				failed.getMessage().contains("must not have null values"),
				"Expected " + failed.getMessage() + " to contain \"must not have null values\""
		);
	}

	@Test
	public void testProperLongValues() throws IOException {
		Longs longs = new Longs();
		longs.array = new Long[] { -1L, 2L };
		longs.set.add(-1234L);

		Longs loaded = BitserHelper.serializeAndDeserialize(new Bitser(false), longs);
		assertArrayEquals(new Long[] { -1L, 2L }, loaded.array);
		assertEquals(1, loaded.set.size());
		assertEquals(-1234L, loaded.set.iterator().next());
	}
}
