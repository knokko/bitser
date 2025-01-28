package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.field.NestedFieldSetting;
import com.github.knokko.bitser.serialize.Bitser;
import org.junit.jupiter.api.Test;

import static com.github.knokko.bitser.wrapper.TestHelper.assertContains;
import static org.junit.jupiter.api.Assertions.*;

public class TestByteCollectionField {

	@BitStruct(backwardCompatible = false)
	private static class BooleanArray {

		@NestedFieldSetting(path = "", writeAsBytes = true)
		boolean[] data;
	}

	@Test
	public void testBooleanArrayMultipleOf8() {
		BooleanArray array = new BooleanArray();
		array.data = new boolean[] {true, false, true, true, true, false, true, false};

		array = new Bitser(true).deepCopy(array);
		assertArrayEquals(new boolean[] {true, false, true, true, true, false, true, false}, array.data);
	}

	@Test
	public void testBooleanArrayNoMultipleOf8() {
		BooleanArray array = new BooleanArray();
		array.data = new boolean[] {true, false, true, true};

		array = new Bitser(false).deepCopy(array);
		assertArrayEquals(new boolean[] {true, false, true, true}, array.data);
	}

	@BitStruct(backwardCompatible = false)
	private static class ByteArray {

		@NestedFieldSetting(path = "", optional = true, writeAsBytes = true)
		byte[] data;
	}

	@Test
	public void testByteArray() {
		ByteArray array = new ByteArray();
		Bitser bitser = new Bitser(false);
		ByteArray loaded = bitser.deepCopy(array);
		assertNull(loaded.data);

		array.data = new byte[0];
		loaded = bitser.deepCopy(array);
		assertEquals(0, loaded.data.length);

		array.data = new byte[]{-128, -1, 0, 1, 127};
		loaded = bitser.deepCopy(array);
		assertArrayEquals(new byte[]{-128, -1, 0, 1, 127}, loaded.data);
	}

	@BitStruct(backwardCompatible = false)
	private static class IntArray {

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
	public void testIntArray() {
		Bitser bitser = new Bitser(true);
		IntArray nullArray = new IntArray(null);
		String errorMessage = assertThrows(
				InvalidBitValueException.class, () -> bitser.serializeToBytes(nullArray)
		).getMessage();
		assertContains(errorMessage, "must not be null");

		IntArray empty = new IntArray(new int[0]);
		IntArray loaded = bitser.deepCopy(empty);
		assert loaded.data != null;
		assertEquals(0, loaded.data.length);

		IntArray filled = new IntArray(new int[]{Integer.MIN_VALUE, -1234, -1, 0, 10, 12345, Integer.MAX_VALUE});
		loaded = bitser.deepCopy(filled);
		assert loaded.data != null;
		assertArrayEquals(new int[]{Integer.MIN_VALUE, -1234, -1, 0, 10, 12345, Integer.MAX_VALUE}, loaded.data);
	}

	@BitStruct(backwardCompatible = false)
	private static class InvalidOptional {

		@SuppressWarnings("unused")
		@NestedFieldSetting(path = "", writeAsBytes = true)
		@NestedFieldSetting(path = "c", optional = true)
		byte[] data = new byte[10];
	}

	@Test
	public void testInvalidOptional() {
		String errorMessage = assertThrows(InvalidBitFieldException.class,
				() -> new Bitser(true).deepCopy(new InvalidOptional())
		).getMessage();
		assertContains(errorMessage, "NestedFieldSetting's on writeAsBytes targets is forbidden:");
	}

	@BitStruct(backwardCompatible = false)
	private static class InvalidAnnotations {

		@SuppressWarnings("unused")
		@IntegerField(expectUniform = false)
		@NestedFieldSetting(path = "", writeAsBytes = true)
		byte[] data = new byte[10];
	}

	@Test
	public void testInvalidAnnotations() {
		String errorMessage = assertThrows(InvalidBitFieldException.class,
				() -> new Bitser(true).deepCopy(new InvalidAnnotations())
		).getMessage();
		assertContains(errorMessage, "Value annotations are forbidden when writeAsBytes is true");
	}
}
