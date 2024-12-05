package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.field.NestedFieldSetting;
import com.github.knokko.bitser.io.BitserHelper;
import com.github.knokko.bitser.serialize.Bitser;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class TestByteCollectionField {

	@BitStruct(backwardCompatible = false)
	private static class BooleanArray {

		@BitField(ordering = 0)
		@NestedFieldSetting(path = "", writeAsBytes = true)
		boolean[] data;
	}

	@Test
	public void testBooleanArrayMultipleOf8() throws IOException {
		BooleanArray array = new BooleanArray();
		array.data = new boolean[] {true, false, true, true, true, false, true, false};

		BooleanArray loaded = BitserHelper.serializeAndDeserialize(new Bitser(true), array);
		assertArrayEquals(new boolean[] {true, false, true, true, true, false, true, false}, loaded.data);
	}

	@Test
	public void testBooleanArrayNoMultipleOf8() throws IOException {
		BooleanArray array = new BooleanArray();
		array.data = new boolean[] {true, false, true, true};

		BooleanArray loaded = BitserHelper.serializeAndDeserialize(new Bitser(false), array);
		assertArrayEquals(new boolean[] {true, false, true, true}, loaded.data);
	}

	@BitStruct(backwardCompatible = false)
	private static class ByteArray {

		@BitField(ordering = 0)
		@NestedFieldSetting(path = "", optional = true, writeAsBytes = true)
		byte[] data;
	}

	@Test
	public void testByteArray() throws IOException {
		ByteArray array = new ByteArray();
		ByteArray loaded = BitserHelper.serializeAndDeserialize(new Bitser(false), array);
		assertNull(loaded.data);

		array.data = new byte[0];
		loaded = BitserHelper.serializeAndDeserialize(new Bitser(false), array);
		assertEquals(0, loaded.data.length);

		array.data = new byte[]{-128, -1, 0, 1, 127};
		loaded = BitserHelper.serializeAndDeserialize(new Bitser(false), array);
		assertArrayEquals(new byte[]{-128, -1, 0, 1, 127}, loaded.data);
	}

	@BitStruct(backwardCompatible = false)
	private static class IntArray {

		@BitField(ordering = 0)
		@NestedFieldSetting(path = "", writeAsBytes = true)
		final int[] data;

		@SuppressWarnings("unused")
		IntArray() {
			this.data = null;
		}

		IntArray(int[] data) {
			this.data = data;
		}
	}

	@Test
	public void testIntArray() throws IOException {
		IntArray nullArray = new IntArray(null);
		InvalidBitValueException failed = assertThrows(
				InvalidBitValueException.class,
				() -> BitserHelper.serializeAndDeserialize(new Bitser(false), nullArray)
		);
		assertTrue(
				failed.getMessage().contains("must not be null"),
				"Expected " + failed.getMessage() + " to contain \"must not be null\""
		);

		IntArray empty = new IntArray(new int[0]);
		IntArray loaded = BitserHelper.serializeAndDeserialize(new Bitser(true), empty);
		assert loaded.data != null;
		assertEquals(0, loaded.data.length);

		IntArray filled = new IntArray(new int[]{Integer.MIN_VALUE, -1234, -1, 0, 10, 12345, Integer.MAX_VALUE});
		loaded = BitserHelper.serializeAndDeserialize(new Bitser(true), filled);
		assert loaded.data != null;
		assertArrayEquals(new int[]{Integer.MIN_VALUE, -1234, -1, 0, 10, 12345, Integer.MAX_VALUE}, loaded.data);
	}

	@BitStruct(backwardCompatible = false)
	private static class InvalidOptional {

		@SuppressWarnings("unused")
		@BitField(ordering = 0)
		@NestedFieldSetting(path = "", writeAsBytes = true)
		@NestedFieldSetting(path = "c", optional = true)
		byte[] data = new byte[10];
	}

	@Test
	public void testInvalidOptional() {
		String errorMessage = assertThrows(InvalidBitFieldException.class,
				() -> BitserHelper.serializeAndDeserialize(new Bitser(true), new InvalidOptional())
		).getMessage();
		assertTrue(
				errorMessage.contains("NestedFieldSetting's on writeAsBytes targets is forbidden:"),
				"Expected " + errorMessage + " to contain \"NestedFieldSetting's on writeAsBytes targets is forbidden:\""
		);
	}

	@BitStruct(backwardCompatible = false)
	private static class InvalidAnnotations {

		@SuppressWarnings("unused")
		@BitField(ordering = 0)
		@IntegerField(expectUniform = false)
		@NestedFieldSetting(path = "", writeAsBytes = true)
		byte[] data = new byte[10];
	}

	@Test
	public void testInvalidAnnotations() {
		InvalidBitFieldException failed = assertThrows(InvalidBitFieldException.class,
				() -> BitserHelper.serializeAndDeserialize(new Bitser(true), new InvalidAnnotations())
		);
		assertTrue(
				failed.getMessage().contains("Value annotations are forbidden when writeAsBytes is true"),
				"Expected " + failed.getMessage() + " to contain \"Value annotations are forbidden when writeAsBytes is true\""
		);
	}
}
