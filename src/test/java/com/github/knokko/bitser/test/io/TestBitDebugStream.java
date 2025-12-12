package com.github.knokko.bitser.test.io;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.Bitser;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.FloatField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.io.BitDebugStream;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class TestBitDebugStream {

	@BitStruct(backwardCompatible = true)
	private static class RootStruct {

		@BitField(id = 0)
		final ArrayList<ListElement> list = new ArrayList<>();

		@BitField(id = 1)
		ArrayElement[] array;

		@BitField(id = 2)
		@IntegerField(expectUniform = false)
		int example;

		@BitField(id = 3)
		final HashMap<MapKey, MapValue> map = new HashMap<>();

		@BitField(id = 4)
		@IntegerField(expectUniform = false)
		int padding;
	}

	@BitStruct(backwardCompatible = true)
	private static class ListElement {

		@BitField(id = 0)
		String description;
	}

	@BitStruct(backwardCompatible = true)
	private static class ArrayElement {

		@BitField(id = 0)
		@FloatField
		float weight;
	}

	@BitStruct(backwardCompatible = true)
	private static class MapKey {

		@BitField(id = 5)
		@IntegerField(expectUniform = true)
		long address;

		@Override
		public int hashCode() {
			return (int) address;
		}

		@Override
		public boolean equals(Object other) {
			return other instanceof MapKey && this.address == ((MapKey) other).address;
		}
	}

	@BitStruct(backwardCompatible = true)
	private static class MapValue {

		@BitField(id = 2)
		final ConcurrentHashMap<MapKey, UUID> nestedMap = new ConcurrentHashMap<>();
	}

	private RootStruct generate() {
		RootStruct root = new RootStruct();
		root.list.add(new ListElement());
		root.list.add(new ListElement());
		root.list.get(0).description = "first";
		root.list.get(1).description = "second";
		root.array = new ArrayElement[] {
				new ArrayElement(),
				new ArrayElement()
		};
		root.array[0].weight = 3f;
		root.array[1].weight = -5f;
		root.example = 100;

		MapKey key1 = new MapKey();
		key1.address = 92;
		MapValue value1 = new MapValue();
		value1.nestedMap.put(key1, new UUID(-1, 1));

		MapKey key2 = new MapKey();
		key2.address = 50;

		root.map.put(key1, value1);
		root.map.put(key2, new MapValue());

		root.padding = 81;
		return root;
	}

	private void check(RootStruct loaded) {
		assertEquals(2, loaded.list.size());
		assertEquals("first", loaded.list.get(0).description);
		assertEquals("second", loaded.list.get(1).description);

		assertEquals(2, loaded.array.length);
		assertEquals(3f, loaded.array[0].weight);
		assertEquals(-5f, loaded.array[1].weight);

		assertEquals(100, loaded.example);

		assertEquals(2, loaded.map.size());

		MapKey key1 = new MapKey();
		key1.address = 92;
		MapValue value1 = loaded.map.get(key1);
		assertEquals(1, value1.nestedMap.size());
		assertEquals(new UUID(-1, 1), value1.nestedMap.get(key1));

		MapKey key2 = new MapKey();
		key2.address = 50;
		assertEquals(0, loaded.map.get(key2).nestedMap.size());
	}

	@Test
	public void testWithoutDebug() {
		Bitser bitser = new Bitser(true);
		RootStruct original = generate();

		check(bitser.deepCopy(original));
		check(bitser.deepCopy(original, Bitser.BACKWARD_COMPATIBLE));
	}

	@Test
	public void testDebugWithoutBackwardCompatibility() throws IOException {
		ByteArrayOutputStream contentBytes = new ByteArrayOutputStream();
		ByteArrayOutputStream debugBytes = new ByteArrayOutputStream();
		PrintWriter writer = new PrintWriter(debugBytes);
		BitDebugStream debug = new BitDebugStream(contentBytes, writer);
		new Bitser(false).serialize(generate(), debug);
		debug.finish();
		writer.flush();
		writer.close();

		check(new Bitser(true).deserializeFromBytes(RootStruct.class, contentBytes.toByteArray()));

		Scanner actualScanner = new Scanner(new ByteArrayInputStream(debugBytes.toByteArray()));
		Scanner expectedScanner = new Scanner(Objects.requireNonNull(
				TestBitDebugStream.class.getResourceAsStream("expected-debug.yaml")
		));

		while (expectedScanner.hasNextLine()) {
			assertEquals(expectedScanner.nextLine(), actualScanner.nextLine());
		}
		assertFalse(actualScanner.hasNextLine());
	}

	@Test
	public void testDebugWithBackwardCompatibility() throws IOException {
		ByteArrayOutputStream contentBytes = new ByteArrayOutputStream();
		ByteArrayOutputStream debugBytes = new ByteArrayOutputStream();
		PrintWriter writer = new PrintWriter(debugBytes);
		BitDebugStream debug = new BitDebugStream(contentBytes, writer);
		new Bitser(false).serialize(generate(), debug, Bitser.BACKWARD_COMPATIBLE);
		debug.finish();
		writer.flush();
		writer.close();

		check(new Bitser(true).deserializeFromBytes(
				RootStruct.class, contentBytes.toByteArray(), Bitser.BACKWARD_COMPATIBLE
		));

		Scanner actualScanner = new Scanner(new ByteArrayInputStream(debugBytes.toByteArray()));
		Scanner expectedScanner = new Scanner(Objects.requireNonNull(
				TestBitDebugStream.class.getResourceAsStream("expected-debug-backward-compatible.yaml")
		));

		//System.out.println(new String(debugBytes.toByteArray()));
		while (expectedScanner.hasNextLine()) {
			assertEquals(expectedScanner.nextLine(), actualScanner.nextLine());
		}
		assertFalse(actualScanner.hasNextLine());
	}
}
