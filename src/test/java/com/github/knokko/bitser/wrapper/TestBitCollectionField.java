package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.field.NestedFieldSetting;
import com.github.knokko.bitser.io.BitCountStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.Bitser;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.*;

import static com.github.knokko.bitser.wrapper.TestHelper.assertContains;
import static org.junit.jupiter.api.Assertions.*;

public class TestBitCollectionField {

	@BitStruct(backwardCompatible = false)
	private static class Strings {

		@NestedFieldSetting(path = "c", optional = true)
		private String[] array;

		@NestedFieldSetting(path = "", optional = true)
		private ArrayList<String> list;
	}

	@Test
	public void testNullStrings() {
		Strings strings = new Strings();
		strings.array = new String[]{"hello", null, "world"};

		strings = new Bitser(true).deepCopy(strings);
		assertArrayEquals(new String[]{"hello", null, "world"}, strings.array);
		assertNull(strings.list);
	}

	@Test
	public void testNonNullStrings() {
		Strings strings = new Strings();
		strings.array = new String[]{"test1234"};
		strings.list = new ArrayList<>();
		strings.list.add("hello, world!");

		strings = new Bitser(false).deepCopy(strings);
		assertArrayEquals(new String[]{"test1234"}, strings.array);
		assertEquals(1, strings.list.size());
		assertEquals("hello, world!", strings.list.get(0));
	}

	@BitStruct(backwardCompatible = false)
	private static class Bytes {

		@IntegerField(expectUniform = true)
		@NestedFieldSetting(path = "", sizeField = @IntegerField(minValue = 2, maxValue = 2, expectUniform = true))
		private final byte[] array = new byte[2];

		@IntegerField(expectUniform = false)
		@NestedFieldSetting(path = "c", optional = true)
		private final LinkedList<Byte> list = new LinkedList<>();
	}

	@Test
	public void testNullAndZeroBytes() {
		Bytes bytes = new Bytes();
		bytes.list.add((byte) 123);
		bytes.list.add(null);
		bytes.list.add((byte) 45);

		bytes = new Bitser(false).deepCopy(bytes);
		assertArrayEquals(new byte[2], bytes.array);
		assertEquals(3, bytes.list.size());
		assertEquals((byte) 123, bytes.list.get(0));
		assertNull(bytes.list.get(1));
		assertEquals((byte) 45, bytes.list.get(2));
	}

	@Test
	public void testNonNullBytes() {
		Bytes bytes = new Bytes();
		bytes.array[0] = -12;
		bytes.array[1] = 34;
		bytes.list.add((byte) -123);

		bytes = new Bitser(true).deepCopy(bytes);
		assertArrayEquals(new byte[]{-12, 34}, bytes.array);
		assertEquals(1, bytes.list.size());
		assertEquals((byte) -123, bytes.list.get(0));
	}

	@BitStruct(backwardCompatible = false)
	private static class InvalidShortArray {

		@SuppressWarnings("unused")
		@IntegerField(expectUniform = false)
		@NestedFieldSetting(path = "c", optional = true)
		private short[] values;
	}

	@Test
	public void testInvalidShortArray() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class,
				() -> new Bitser(true).serializeToBytes(new InvalidShortArray())
		).getMessage();
		assertContains(errorMessage, "can't be optional");
	}

	@BitStruct(backwardCompatible = false)
	private static class Longs {

		@IntegerField(expectUniform = false)
		@NestedFieldSetting(path = "", optional = true)
		@NestedFieldSetting(path = "c", optional = true)
		public Long[] array;

		@IntegerField(expectUniform = false)
		public HashSet<Long> set = new HashSet<>();
	}

	@Test
	public void testNullLongArray() {
		Longs longs = new Longs();
		longs = new Bitser(false).deepCopy(longs);
		assertNull(longs.array);
		assertEquals(0, longs.set.size());
	}

	@Test
	public void testNullLongValues() {
		Longs longs = new Longs();
		longs.array = new Long[]{12L, null, 34L};
		longs = new Bitser(false).deepCopy(longs);
		assertArrayEquals(new Long[]{12L, null, 34L}, longs.array);
		assertEquals(0, longs.set.size());
	}

	@Test
	public void testForbidLongSetNull() {
		Longs longs = new Longs();
		longs.set = null;
		String errorMessage = assertThrows(
				InvalidBitValueException.class,
				() -> new Bitser(true).serialize(longs, new BitOutputStream(new ByteArrayOutputStream()))
		).getMessage();
		assertContains(errorMessage, "must not be null");
	}

	@Test
	public void testForbidLongSetNullValues() {
		Longs longs = new Longs();
		longs.set.add(12L);
		longs.set.add(null);
		longs.set.add(34L);
		String errorMessage = assertThrows(
				InvalidBitValueException.class,
				() -> new Bitser(true).serialize(longs, new BitOutputStream(new ByteArrayOutputStream()))
		).getMessage();
		assertContains(errorMessage, "must not have null elements");
	}

	@Test
	public void testProperLongValues() {
		Longs longs = new Longs();
		longs.array = new Long[]{-1L, 2L};
		longs.set.add(-1234L);

		longs = new Bitser(true).deepCopy(longs);
		assertArrayEquals(new Long[]{-1L, 2L}, longs.array);
		assertEquals(1, longs.set.size());
		assertEquals(-1234L, longs.set.iterator().next());
	}

	@BitStruct(backwardCompatible = false)
	private static class MissingGenerics {

		@BitField
		@SuppressWarnings({"rawtypes", "unused"})
		ArrayList list = new ArrayList();
	}

	@Test
	public void testMissingGenerics() {
		String errorMessage = assertThrows(InvalidBitFieldException.class,
				() -> new Bitser(true).serialize(new MissingGenerics(), new BitCountStream())
		).getMessage();
		assertContains(errorMessage, "Unexpected generic type");
	}

	@BitStruct(backwardCompatible = false)
	private static class UnknownGenerics {

		@BitField
		@SuppressWarnings({"rawtypes", "unused"})
		ArrayList<?> list = new ArrayList();
	}

	@Test
	public void testUnknownGenerics() {
		String errorMessage = assertThrows(InvalidBitFieldException.class,
				() -> new Bitser(true).serialize(new UnknownGenerics(), new BitCountStream())
		).getMessage();
		assertContains(errorMessage, "Unexpected generic type");
	}

	@BitStruct(backwardCompatible = false)
	private static class WithAbstractList {

		@BitField
		@SuppressWarnings("unused")
		List<String> list = new ArrayList<>();
	}

	@Test
	public void testAbstractList() {
		String errorMessage = assertThrows(InvalidBitFieldException.class,
				() -> new Bitser(true).serialize(new WithAbstractList(), new BitCountStream())
		).getMessage();
		assertContains(errorMessage, "Field type must not be abstract or an interface");
		assertContains(errorMessage, "WithAbstractList.list");
	}
}
