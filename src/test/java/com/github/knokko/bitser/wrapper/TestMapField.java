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
import java.util.HashMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

public class TestMapField {

	@BitStruct(backwardCompatible = false)
	static class StringMap {

		@BitField(ordering = 0)
		final ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
	}

	@Test
	public void testStringMap() throws IOException {
		assertEquals(0, BitserHelper.serializeAndDeserialize(new Bitser(false), new StringMap()).map.size());

		StringMap stringMap = new StringMap();
		stringMap.map.put("hello", "world");
		stringMap.map.put("hi", "triangle");

		StringMap loaded = BitserHelper.serializeAndDeserialize(new Bitser(true), stringMap);
		assertEquals(2, loaded.map.size());
		assertEquals("world", loaded.map.get("hello"));
		assertEquals("triangle", loaded.map.get("hi"));
	}

	@BitStruct(backwardCompatible = false)
	static class IntMap {

		@SuppressWarnings("unused")
		@IntegerField(expectUniform = true, minValue = 10, maxValue = 20)
		private static final boolean KEY = false;

		@BitField(ordering = 0)
		@IntegerField(expectUniform = true, minValue = 50, maxValue = 60)
		@NestedFieldSetting(path = "", sizeField = @IntegerField(expectUniform = true, minValue = 0, maxValue = 3))
		@NestedFieldSetting(path = "k", fieldName = "KEY")
		TreeMap<Integer, Byte> map;
	}

	@Test
	public void testIntMap() throws IOException {
		IntMap intMap = new IntMap();
		intMap.map = new TreeMap<>();
		intMap.map.put(10, (byte) 50);

		IntMap loaded = BitserHelper.serializeAndDeserialize(new Bitser(true), intMap);
		assertEquals(1, loaded.map.size());
		assertEquals((byte) 50, loaded.map.get(10));

		// 2 bits for the size
		// 4 bits for the key
		// 4 bits for the value
		BitCountStream counter = new BitCountStream();
		new Bitser(true).serialize(intMap, counter);
		assertEquals(10, counter.getCounter());
	}

	@BitStruct(backwardCompatible = false)
	static class KeyFieldMap {

		@SuppressWarnings("unused")
		@IntegerField(expectUniform = false, minValue = 100)
		private static final boolean KEY = false;

		@BitField(ordering = 0)
		@NestedFieldSetting(path = "k", optional = true, fieldName = "KEY")
		final HashMap<Integer, String> map = new HashMap<>();
	}

	@Test
	public void testKeyFieldMap() throws IOException {
		KeyFieldMap keyMap = new KeyFieldMap();
		keyMap.map.put(null, "hello");
		keyMap.map.put(150, "world");

		KeyFieldMap loaded = BitserHelper.serializeAndDeserialize(new Bitser(false), keyMap);
		assertEquals(2, loaded.map.size());
		assertEquals("hello", loaded.map.get(null));
		assertEquals("world", loaded.map.get(150));
	}

	@BitStruct(backwardCompatible = false)
	static class ImplicitValueFieldMap {

		@BitField(ordering = 0)
		@IntegerField(expectUniform = true, minValue = 10, maxValue = 11)
		final HashMap<String, Integer> map = new HashMap<>();
	}

	@Test
	public void testImplicitValueFieldMap() throws IOException {
		ImplicitValueFieldMap valueMap = new ImplicitValueFieldMap();
		valueMap.map.put("price", 11);
		valueMap.map.put("weight", 10);

		ImplicitValueFieldMap loaded = BitserHelper.serializeAndDeserialize(new Bitser(false), valueMap);
		assertEquals(2, loaded.map.size());
		assertEquals(11, loaded.map.get("price"));
		assertEquals(10, loaded.map.get("weight"));
	}

	@BitStruct(backwardCompatible = false)
	static class ExplicitValueFieldMap {

		@SuppressWarnings("unused")
		@IntegerField(expectUniform = true, minValue = 10, maxValue = 11)
		private static final boolean VALUE = false;

		@BitField(ordering = 0)
		@NestedFieldSetting(path = "v", fieldName = "VALUE", optional = true)
		final TreeMap<String, Integer> map = new TreeMap<>();
	}

	@Test
	public void testExplicitValueFieldMap() throws IOException {
		ExplicitValueFieldMap valueMap = new ExplicitValueFieldMap();
		valueMap.map.put("knokko", 10);
		valueMap.map.put("knok", null);

		ExplicitValueFieldMap loaded = BitserHelper.serializeAndDeserialize(new Bitser(true), valueMap);
		assertEquals(2, loaded.map.size());
		assertEquals(10, loaded.map.get("knokko"));
		assertTrue(loaded.map.containsKey("knok"));
		assertNull(loaded.map.get("knok"));
	}

	@BitStruct(backwardCompatible = false)
	static class OptionalMap {

		@BitField(ordering = 0)
		@NestedFieldSetting(path = "", optional = true)
		TreeMap<String, String> map;
	}

	@Test
	public void testOptionalMap() throws IOException {
		assertNull(BitserHelper.serializeAndDeserialize(new Bitser(true), new OptionalMap()).map);
	}

	@BitStruct(backwardCompatible = false)
	static class ReferenceMaps {

		@SuppressWarnings("unused")
		@ReferenceFieldTarget(label = "test")
		private static final boolean KEY1 = false;

		@SuppressWarnings("unused")
		@ReferenceField(stable = false, label = "test")
		private static final boolean KEY2 = true;

		@SuppressWarnings("unused")
		@ReferenceField(stable = true, label = "test")
		private static final boolean VALUE = true;

		@BitField(ordering = 0)
		@NestedFieldSetting(path = "k", fieldName = "KEY1")
		@NestedFieldSetting(path = "v", fieldName = "VALUE")
		final ConcurrentHashMap<ReferenceMaps, ReferenceMaps> map1 = new ConcurrentHashMap<>();

		@BitField(ordering = 1)
		@NestedFieldSetting(path = "k", fieldName = "KEY2")
		@ReferenceFieldTarget(label = "test")
		final ConcurrentHashMap<ReferenceMaps, ReferenceMaps> map2 = new ConcurrentHashMap<>();

		@BitField(ordering = 2)
		@StableReferenceFieldId
		final UUID id = UUID.randomUUID();
	}

	@Test
	public void testReferenceMaps() throws IOException {
		ReferenceMaps inner1 = new ReferenceMaps();
		ReferenceMaps inner2 = new ReferenceMaps();
		ReferenceMaps inner3 = new ReferenceMaps();
		inner2.map1.put(inner3, inner2);
		inner2.map2.put(inner1, inner1);

		ReferenceMaps outer = new ReferenceMaps();
		outer.map2.put(inner1, inner2);

		ReferenceMaps loaded = BitserHelper.serializeAndDeserialize(new Bitser(false), outer);
		assertEquals(0, loaded.map1.size());
		assertEquals(1, loaded.map2.size());

		ReferenceMaps loaded1 = loaded.map2.keys().nextElement();
		assertEquals(0, loaded1.map1.size());
		assertEquals(0, loaded1.map2.size());

		ReferenceMaps loaded2 = loaded.map2.get(loaded1);
		assertEquals(1, loaded2.map1.size());
		assertEquals(1, loaded2.map2.size());

		assertSame(loaded2, loaded2.map1.values().iterator().next());
		assertNotSame(loaded1, loaded2.map1.keys().nextElement());
		assertSame(loaded1, loaded2.map2.get(loaded1));

		assertEquals(outer.id, loaded.id);
		assertEquals(inner1.id, loaded1.id);
		assertEquals(inner2.id, loaded2.id);
	}

	@BitStruct(backwardCompatible = false)
	static class WriteAsBytesMap {

		@SuppressWarnings("unused")
		@BitField(ordering = 0)
		@NestedFieldSetting(path = "", writeAsBytes = true)
		final TreeMap<Byte, Byte> map = new TreeMap<>();
	}

	@Test
	public void testWriteAsBytesMap() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class,
				() -> new Bitser(false).serialize(new WriteAsBytesMap(), new BitCountStream())
		).getMessage();
		assertTrue(
				errorMessage.contains("writeAsBytes is not allowed on Maps"),
				"Expected " + errorMessage + " to contain \"writeAsBytes is not allowed on Maps\""
		);
	}

	@Test
	public void testNullKeysAreForbiddenByDefault() {
		ImplicitValueFieldMap valueMap = new ImplicitValueFieldMap();
		valueMap.map.put(null, 12);

		String errorMessage = assertThrows(
				InvalidBitValueException.class,
				() -> new Bitser(false).serialize(valueMap, new BitCountStream())
		).getMessage();
		assertTrue(
				errorMessage.contains("must not have null keys")
		);
	}

	@Test
	public void testNullValuesAreForbiddenByDefault() {
		KeyFieldMap keyMap = new KeyFieldMap();
		keyMap.map.put(1234, null);

		String errorMessage = assertThrows(
				InvalidBitValueException.class,
				() -> new Bitser(true).serialize(keyMap, new BitCountStream())
		).getMessage();
		assertTrue(
				errorMessage.contains("must not have null values"),
				"Expected " + errorMessage + " to contain \"must not have null values\""
		);
	}

	@Test
	public void testNullMapsAreForbiddenByDefault() {
		String errorMessage = assertThrows(
				InvalidBitValueException.class,
				() -> new Bitser(true).serialize(new IntMap(), new BitCountStream())
		).getMessage();
		assertTrue(
				errorMessage.contains("must not be null"),
				"Expected " + errorMessage + " to contain \"must not be null\""
		);
	}

	@BitStruct(backwardCompatible = false)
	static class BadOptionalMap {

		@SuppressWarnings("unused")
		@BitField(ordering = 0, optional = true)
		HashMap<String, String> map;
	}

	@Test
	public void testBadOptionalMap() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class,
				() -> new Bitser(false).serialize(new BadOptionalMap(), new BitCountStream())
		).getMessage();
		assertTrue(
				errorMessage.contains("optional BitField is not allowed"),
				"Expected " + errorMessage + " to contain \"optional BitField is not allowed\""
		);
		assertTrue(
				errorMessage.contains("use @NestedFieldSetting instead"),
				"Expected " + errorMessage + " to contain \"use @NestedFieldSetting instead\""
		);
	}
}
