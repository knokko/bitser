package com.github.knokko.bitser.backward;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.FloatField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.field.NestedFieldSetting;
import com.github.knokko.bitser.serialize.Bitser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestCollectionBackwardCompatibility {

	@BitStruct(backwardCompatible = true)
	private static class ShallowBefore {

		@SuppressWarnings("unused")
		@IntegerField(expectUniform = false)
		private static final boolean KEY_PROPERTIES = false;

		@SuppressWarnings("unused")
		@FloatField
		private static final boolean VALUE_PROPERTIES = false;

		@BitField(id = 2)
		@NestedFieldSetting(path = "k", fieldName = "KEY_PROPERTIES")
		@NestedFieldSetting(path = "v", fieldName = "VALUE_PROPERTIES", optional = true)
		final HashMap<Integer, Float> sinTable = new HashMap<>();

		@BitField(id = 5)
		@NestedFieldSetting(path = "", writeAsBytes = true)
		short[] heights;

		@BitField(id = 4)
		@NestedFieldSetting(path = "", optional = true)
		@NestedFieldSetting(path = "c", optional = true)
		final ArrayList<String> lines = new ArrayList<>();
	}

	@BitStruct(backwardCompatible = true)
	private static class ShallowAfter {

		@SuppressWarnings("unused")
		@IntegerField(expectUniform = true)
		private static final boolean KEY_PROPERTIES = false;

		@SuppressWarnings("unused")
		@FloatField(expectMultipleOf = 10.0)
		private static final boolean VALUE_PROPERTIES = false;

		@BitField(id = 2)
		@NestedFieldSetting(path = "", optional = true)
		@NestedFieldSetting(path = "k", fieldName = "KEY_PROPERTIES")
		@NestedFieldSetting(path = "v", fieldName = "VALUE_PROPERTIES", optional = true)
		final HashMap<Integer, Float> sinTable = new HashMap<>();

		@BitField(id = 5)
		@NestedFieldSetting(path = "", writeAsBytes = true)
		int[] heights;

		@BitField(id = 4)
		@NestedFieldSetting(path = "")
		@NestedFieldSetting(path = "c", optional = true)
		final ArrayList<String> lines = new ArrayList<>();
	}

	@Test
	public void testShallowCollectionsBackwardCompatibility() {
		Bitser bitser = new Bitser(false);
		ShallowBefore before = new ShallowBefore();
		before.sinTable.put(0, 0f);
		before.sinTable.put(90, 1f);
		before.sinTable.put(180, 0f);
		before.sinTable.put(270, -1f);
		before.sinTable.put(1234, null);
		before.heights = new short[] {123, -76, 43, 12345};
		before.lines.add("shallow");
		before.lines.add(null);
		before.lines.add("compatibility");

		ShallowAfter after = bitser.deserializeFromBytes(ShallowAfter.class, bitser.serializeToBytes(before, Bitser.BACKWARD_COMPATIBLE), Bitser.BACKWARD_COMPATIBLE);
		assertEquals(before.sinTable, after.sinTable);
		assertArrayEquals(new int[] {123, -76, 43, 12345}, after.heights);
		assertEquals(before.lines, after.lines);

		ShallowBefore back = bitser.deserializeFromBytes(ShallowBefore.class, bitser.serializeToBytes(after, Bitser.BACKWARD_COMPATIBLE), Bitser.BACKWARD_COMPATIBLE);
		assertEquals(before.sinTable, back.sinTable);
		assertArrayEquals(before.heights, back.heights);
		assertEquals(before.lines, back.lines);
	}
}
