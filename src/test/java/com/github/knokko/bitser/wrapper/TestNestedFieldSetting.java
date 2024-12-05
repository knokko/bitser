package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.field.*;
import com.github.knokko.bitser.io.BitCountStream;
import com.github.knokko.bitser.io.BitserHelper;
import com.github.knokko.bitser.serialize.Bitser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

public class TestNestedFieldSetting {

	@BitStruct(backwardCompatible = false)
	static class AlternatingOptionalCollection {

		@BitField(ordering = 0)
		@FloatField(expectMultipleOf = 0.5)
		@NestedFieldSetting(path = "cc", optional = true)
		@NestedFieldSetting(path = "ccc", optional = true)
		ArrayList<HashSet<HashSet<LinkedList<Float>>>> nested = new ArrayList<>();

		@BitField(ordering = 1)
		String test;
	}

	@Test
	@SuppressWarnings("OptionalGetWithoutIsPresent")
	public void testAlternatingDepthOptionalCollection() throws IOException {
		LinkedList<Float> floatList = new LinkedList<>();
		floatList.add(1f);
		floatList.add(5f);

		HashSet<LinkedList<Float>> listSet = new HashSet<>();
		listSet.add(floatList);
		listSet.add(null);

		HashSet<HashSet<LinkedList<Float>>> hashSet = new HashSet<>();
		hashSet.add(null);
		hashSet.add(listSet);

        AlternatingOptionalCollection alternating = new AlternatingOptionalCollection();
		alternating.nested = new ArrayList<>();
		alternating.nested.add(hashSet);
		alternating.test = "test1234";

		AlternatingOptionalCollection loaded = BitserHelper.serializeAndDeserialize(new Bitser(true), alternating);
		assertEquals("test1234", loaded.test);
		assertEquals(1, loaded.nested.size());

		HashSet<HashSet<LinkedList<Float>>> loadedHashSet = loaded.nested.get(0);
		assertEquals(2, loadedHashSet.size());
		assertTrue(loadedHashSet.contains(null));

		HashSet<LinkedList<Float>> loadedListSet = loadedHashSet.stream().filter(Objects::nonNull).findAny().get();
		assertEquals(2, loadedListSet.size());
		assertTrue(loadedListSet.contains(null));

		LinkedList<Float> loadedLinkedList = loadedListSet.stream().filter(Objects::nonNull).findAny().get();
		assertEquals(2, loadedLinkedList.size());
		assertTrue(loadedLinkedList.contains(1f));
		assertTrue(loadedLinkedList.contains(5f));
	}

	@BitStruct(backwardCompatible = false)
	static class AlternatingOptionalArray {

		@BitField(ordering = 0)
		@FloatField(expectMultipleOf = 0.5)
		@NestedFieldSetting(path = "cc", optional = true)
		@NestedFieldSetting(path = "ccc", optional = true)
		float[][][][] nested;

		@BitField(ordering = 1)
		String test;
	}

	@Test
	public void testAlternatingOptionalArray() throws IOException {
		AlternatingOptionalArray alternating = new AlternatingOptionalArray();
		float[][][][] nested = {
				{
						null,
						{
								null,
								{ 3f }
						}
				}
		};
		alternating.nested = nested;
		alternating.test = "ok";

		AlternatingOptionalArray loaded = BitserHelper.serializeAndDeserialize(new Bitser(false), alternating);
		assertNotSame(nested, loaded.nested);
		assertEquals(1, loaded.nested.length);
		assertEquals(2, loaded.nested[0].length);
		assertNull(loaded.nested[0][0]);
		assertEquals(2, loaded.nested[0][1].length);
		assertNull(loaded.nested[0][1][0]);
		assertEquals(1, loaded.nested[0][1][1].length);
		assertEquals(3f, loaded.nested[0][1][1][0], 1e-4f);
	}

	@BitStruct(backwardCompatible = false)
	static class AlternatingMadness {

		@BitField(ordering = 0)
		@IntegerField(expectUniform = true, minValue = 10, maxValue = 15)
		@NestedFieldSetting(path = "", optional = true)
		@NestedFieldSetting(path = "c", optional = true)
		ArrayList<HashSet<LinkedList<int[]>>[]> nested;
	}

	@Test
	public void testAlternatingMadnessNull() throws IOException {
		assertNull(BitserHelper.serializeAndDeserialize(new Bitser(true), new AlternatingMadness()).nested);
	}

	@Test
	public void testAlternatingMadness() throws IOException {
		LinkedList<int[]> list = new LinkedList<>();
		list.add(new int[] { 11, 12 });

		HashSet<LinkedList<int[]>> set = new HashSet<>();
		set.add(list);
		@SuppressWarnings("unchecked")
		HashSet<LinkedList<int[]>>[] setArray = new HashSet[1];
		setArray[0] = set;

		AlternatingMadness alternating = new AlternatingMadness();
		alternating.nested = new ArrayList<>();
		alternating.nested.add(null);
		alternating.nested.add(setArray);

		AlternatingMadness loaded = BitserHelper.serializeAndDeserialize(new Bitser(true), alternating);
		assertEquals(2, loaded.nested.size());
		assertNull(loaded.nested.get(0));
		assertEquals(1, loaded.nested.get(1).length);

		HashSet<LinkedList<int[]>> loadedSet = loaded.nested.get(1)[0];
		assertEquals(1, loadedSet.size());
		LinkedList<int[]> loadedList = loadedSet.iterator().next();
		assertEquals(1, loadedList.size());

		assertArrayEquals(new int[] { 11, 12 }, loadedList.get(0));
	}

	@BitStruct(backwardCompatible = false)
	static class WithCustomSizes {

		@BitField(ordering = 0)
		@NestedFieldSetting(path = "", sizeField = @IntegerField(expectUniform = true, minValue = 1, maxValue = 2))
		@NestedFieldSetting(path = "c", sizeField = @IntegerField(expectUniform = true, minValue = 0, maxValue = 15))
		final ArrayList<LinkedList<Boolean>> list = new ArrayList<>();
	}

	@Test
	public void testWithCustomSizes() throws IOException {
		LinkedList<Boolean> innerList = new LinkedList<>();
		innerList.add(true);
		innerList.add(true);
		WithCustomSizes sizes = new WithCustomSizes();
		sizes.list.add(innerList);

		WithCustomSizes loaded = BitserHelper.serializeAndDeserialize(new Bitser(false), sizes);
		assertEquals(1, loaded.list.size());
		LinkedList<Boolean> loadedList = loaded.list.get(0);
		assertEquals(2, loadedList.size());
		assertTrue(loadedList.get(0));
		assertTrue(loadedList.get(1));

		// 1 bit to store the size of the outer list
		// 4 bits to store the size of the inner list
		// 2 bits to store the boolean values
		BitCountStream counter = new BitCountStream();
		new Bitser(true).serialize(sizes, counter);
		assertEquals(7, counter.getCounter());
	}

	@BitStruct(backwardCompatible = false)
	static class ReferenceLists1 {

		@SuppressWarnings("unused")
		@ReferenceFieldTarget(label = "test")
		private static final boolean TARGET_LIST = false;

		@SuppressWarnings("unused")
		@ReferenceField(stable = false, label = "test")
		private static final boolean REFERENCE_LIST = false;

		@BitField(ordering = 0)
		@NestedFieldSetting(path = "c", fieldName = "TARGET_LIST")
		final LinkedList<LinkedList<String>> targetList = new LinkedList<>();

		@BitField(ordering = 1)
		@NestedFieldSetting(path = "", fieldName = "REFERENCE_LIST")
		LinkedList<String> referenceList;
	}

	@Test
	public void testReferenceLists1() throws IOException {
		LinkedList<String> list1 = new LinkedList<>();
		list1.add("hello");

		LinkedList<String> list2 = new LinkedList<>();
		list2.add("world");

		ReferenceLists1 referenceLists = new ReferenceLists1();
		referenceLists.targetList.add(list1);
		referenceLists.targetList.add(list2);
		referenceLists.referenceList = list2;

		ReferenceLists1 loaded = BitserHelper.serializeAndDeserialize(new Bitser(true), referenceLists);
		assertEquals(2, loaded.targetList.size());
		assertEquals(1, loaded.targetList.get(0).size());
		assertEquals("hello", loaded.targetList.get(0).get(0));
		assertEquals(1, loaded.targetList.get(1).size());
		assertEquals("world", loaded.targetList.get(1).get(0));
		assertSame(loaded.targetList.get(1), loaded.referenceList);
	}

	@BitStruct(backwardCompatible = false)
	static class ReferenceLists2 {

		@SuppressWarnings("unused")
		@ReferenceFieldTarget(label = "test")
		private static final boolean TARGET_LIST = false;

		@SuppressWarnings("unused")
		@ReferenceField(stable = false, label = "test")
		private static final boolean REFERENCE_LIST = false;

		@BitField(ordering = 0)
		@NestedFieldSetting(path = "", fieldName = "TARGET_LIST")
		final LinkedList<String> targetList = new LinkedList<>();

		@BitField(ordering = 1)
		@NestedFieldSetting(path = "c", fieldName = "REFERENCE_LIST")
		final LinkedList<LinkedList<String>> referenceList = new LinkedList<>();
	}

	@Test
	public void testReferenceLists2() throws IOException {
		ReferenceLists2 referenceLists = new ReferenceLists2();
		referenceLists.targetList.add("hello");
		referenceLists.referenceList.add(referenceLists.targetList);

		ReferenceLists2 loaded = BitserHelper.serializeAndDeserialize(new Bitser(true), referenceLists);
		assertEquals(1, loaded.targetList.size());
		assertEquals("hello", loaded.targetList.get(0));

		assertEquals(1, loaded.referenceList.size());
		assertSame(loaded.targetList, loaded.referenceList.get(0));
	}

	@BitStruct(backwardCompatible = false)
	static class ReferenceLists3 {

		@SuppressWarnings("unused")
		@ReferenceFieldTarget(label = "test")
		private static final boolean TARGET = false;

		@SuppressWarnings("unused")
		@ReferenceField(stable = false, label = "test")
		private static final boolean REFERENCE = false;

		@BitField(ordering = 0)
		@NestedFieldSetting(path = "c", fieldName = "TARGET")
		final LinkedList<String> targetList1 = new LinkedList<>();

		@BitField(ordering = 1)
		@ReferenceFieldTarget(label = "test")
		final LinkedList<String> targetList2 = new LinkedList<>();

		@BitField(ordering = 2)
		@NestedFieldSetting(path = "cc", fieldName = "REFERENCE")
		final LinkedList<LinkedList<String>> referenceList1 = new LinkedList<>();

		@BitField(ordering = 3)
		@ReferenceField(stable = false, label = "test")
		final LinkedList<String> referenceList2 = new LinkedList<>();
	}

	@Test
	public void testReferenceLists3() throws IOException {
		ReferenceLists3 referenceLists = new ReferenceLists3();
		referenceLists.targetList1.add("hello");
		referenceLists.targetList2.add("world");
		referenceLists.referenceList1.add(new LinkedList<>());
		referenceLists.referenceList1.get(0).add(referenceLists.targetList1.get(0));
		referenceLists.referenceList1.get(0).add(referenceLists.targetList2.get(0));
		referenceLists.referenceList2.add(referenceLists.targetList1.get(0));

		ReferenceLists3 loaded = BitserHelper.serializeAndDeserialize(new Bitser(false), referenceLists);
		assertEquals(1, loaded.targetList1.size());
		assertEquals("hello", loaded.targetList1.get(0));
		assertEquals(1, loaded.targetList2.size());
		assertEquals("world", loaded.targetList2.get(0));

		assertEquals(1, loaded.referenceList1.size());
		assertEquals(2, loaded.referenceList1.get(0).size());
		assertSame(loaded.targetList1.get(0), loaded.referenceList1.get(0).get(0));
		assertSame(loaded.targetList2.get(0), loaded.referenceList1.get(0).get(1));

		assertEquals(1, loaded.referenceList2.size());
		assertEquals(loaded.targetList1.get(0), loaded.referenceList2.get(0));
	}

	@BitStruct(backwardCompatible = false)
	static class DuplicateNestedFieldSettings1 {

		@SuppressWarnings("unused")
		@BitField(ordering = 0)
		@NestedFieldSetting(path = "")
		@NestedFieldSetting(path = "")
		final ArrayList<LinkedList<String>> badList = new ArrayList<>();
	}

	@Test
	public void testDuplicateNestedFieldSettings1() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class,
				() -> new Bitser(true).serialize(new DuplicateNestedFieldSettings1(), new BitCountStream())
		).getMessage();
		assertTrue(
				errorMessage.contains("Multiple NestedFieldSetting's for path "),
				"Expected " + errorMessage + " to contain \"Multiple NestedFieldSetting's for path \""
		);
	}

	@BitStruct(backwardCompatible = false)
	static class DuplicateNestedFieldSettings2 {

		@SuppressWarnings("unused")
		@BitField(ordering = 0)
		@NestedFieldSetting(path = "c", writeAsBytes = true)
		@NestedFieldSetting(path = "cc")
		@NestedFieldSetting(path = "cc")
		final ArrayList<LinkedList<Byte>> badList = new ArrayList<>();
	}

	@Test
	public void testDuplicateNestedFieldSettings2() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class,
				() -> new Bitser(true).serialize(new DuplicateNestedFieldSettings2(), new BitCountStream())
		).getMessage();
		assertTrue(
				errorMessage.contains("Multiple NestedFieldSetting's for path cc"),
				"Expected " + errorMessage + " to contain \"Multiple NestedFieldSetting's for path cc\""
		);
	}

	@BitStruct(backwardCompatible = false)
	static class OptionalBitField {

		@SuppressWarnings("unused")
		@BitField(ordering = 0, optional = true)
		final ArrayList<String> test = new ArrayList<>();
	}

	@Test
	public void testForbidOptionalBitFields() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class,
				() -> new Bitser(true).serialize(new OptionalBitField(), new BitCountStream())
		).getMessage();
		assertTrue(
				errorMessage.contains("optional BitField is not allowed on collection field"),
				"Expected " + errorMessage + " to contain \"optional BitField is not allowed on collection field\""
		);
		assertTrue(
				errorMessage.contains("use @NestedFieldSetting instead"),
				"Expected " + errorMessage + " to contain \"use @NestedFieldSetting instead\""
		);
	}

	@Test
	public void testInvalidAlternatingOptionalCollections() {
		AlternatingOptionalCollection alternating = new AlternatingOptionalCollection();
		alternating.nested.add(null);

		String error1 = assertThrows(
				InvalidBitValueException.class,
				() -> new Bitser(true).serialize(alternating, new BitCountStream())
		).getMessage();
		assertTrue(
				error1.contains("must not have null values"),
				"Expected " + error1 + " to contain \"must not have null values\""
		);

		LinkedList<Float> floatList = new LinkedList<>();
		floatList.add(null);
		floatList.add(5f);

		HashSet<LinkedList<Float>> listSet = new HashSet<>();
		listSet.add(floatList);

		HashSet<HashSet<LinkedList<Float>>> hashSet = new HashSet<>();
		hashSet.add(listSet);

		alternating.nested.clear();
		alternating.nested.add(hashSet);

		String error2 = assertThrows(
				InvalidBitValueException.class,
				() -> new Bitser(true).serialize(alternating, new BitCountStream())
		).getMessage();
		assertTrue(
				error2.contains("must not have null values"),
				"Expected " + error2 + " to contain \"must not have null values\""
		);

		alternating.nested = null;
		String error3 = assertThrows(
				InvalidBitValueException.class,
				() -> new Bitser(true).serialize(alternating, new BitCountStream())
		).getMessage();
		assertTrue(
				error3.contains("must not be null"),
				"Expected " + error3 + " to contain \"must not be null\""
		);
	}

	@Test
	public void testInvalidAlternatingOptionalArray() {
		AlternatingOptionalArray alternating = new AlternatingOptionalArray();
		alternating.nested = new float[][][][] {
				null
		};

		String error1 = assertThrows(
				InvalidBitValueException.class,
				() -> new Bitser(true).serialize(alternating, new BitCountStream())
		).getMessage();
		assertTrue(
				error1.contains("must not have null values"),
				"Expected " + error1 + " to contain \"must not have null values\""
		);

		alternating.nested = null;
		String error2 = assertThrows(
				InvalidBitValueException.class,
				() -> new Bitser(true).serialize(alternating, new BitCountStream())
		).getMessage();
		assertTrue(
				error2.contains("must not be null"),
				"Expected " + error2 + " to contain \"must not be null\""
		);
	}
}
