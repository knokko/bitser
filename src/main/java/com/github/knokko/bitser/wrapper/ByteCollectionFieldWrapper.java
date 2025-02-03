package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.serialize.ReadJob;
import com.github.knokko.bitser.serialize.WriteJob;
import com.github.knokko.bitser.util.VirtualField;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.function.Consumer;

import static java.lang.Byte.toUnsignedInt;

@BitStruct(backwardCompatible = false)
class ByteCollectionFieldWrapper extends AbstractCollectionFieldWrapper {

	ByteCollectionFieldWrapper(VirtualField field, IntegerField sizeField) {
		super(field, sizeField);
	}

	@SuppressWarnings("unused")
	private ByteCollectionFieldWrapper() {
		super();
	}

	@Override
	AbstractCollectionFieldWrapper.ArrayType determineArrayType() {
		if (field.type == boolean[].class) return ArrayType.BOOLEAN;
		if (field.type == byte[].class) return ArrayType.BYTE;
		if (field.type == short[].class) return ArrayType.SHORT;
		if (field.type == char[].class) return ArrayType.CHAR;
		if (field.type == int[].class) return ArrayType.INT;
		if (field.type == float[].class) return ArrayType.FLOAT;
		if (field.type == long[].class) return ArrayType.LONG;
		if (field.type == double[].class) return ArrayType.DOUBLE;
		throw new Error("Unexpected write-as-bytes field type " + field.type);
	}

	@Override
	void writeValue(Object value, int size, WriteJob write) throws IOException {
		if (value instanceof boolean[]) write.output.write(toByteArray((boolean[]) value));
		else if (value instanceof byte[]) write.output.write((byte[]) value);
		else if (value instanceof short[]) write.output.write(toByteArray((short[]) value));
		else if (value instanceof int[]) write.output.write(toByteArray((int[]) value));
		else if (value instanceof long[]) write.output.write(toByteArray((long[]) value));
		else throw new UnsupportedOperationException("Can't encode " + value.getClass() + " as bytes");
	}

	private byte[] toByteArray(boolean[] booleans) {
		int numBytes = booleans.length / 8;
		if (booleans.length % 8 != 0) numBytes += 1;

		byte[] bytes = new byte[numBytes];
		for (int boolIndex = 0; boolIndex < booleans.length; boolIndex++) {
			if (booleans[boolIndex]) bytes[boolIndex >> 3] |= (byte) (1 << (boolIndex & 7));
		}
		return bytes;
	}

	private byte[] toByteArray(short[] shorts) {
		byte[] bytes = new byte[2 * shorts.length];
		for (int index = 0; index < shorts.length; index++) {
			short element = shorts[index];
			int byteIndex = 2 * index;
			bytes[byteIndex] = (byte) (element >> 8);
			bytes[byteIndex + 1] = (byte) element;
		}
		return bytes;
	}

	private byte[] toByteArray(int[] ints) {
		byte[] bytes = new byte[4 * ints.length];
		for (int index = 0; index < ints.length; index++) {
			int element = ints[index];
			int byteIndex = 4 * index;
			bytes[byteIndex] = (byte) (element >> 24);
			bytes[byteIndex + 1] = (byte) (element >> 16);
			bytes[byteIndex + 2] = (byte) (element >> 8);
			bytes[byteIndex + 3] = (byte) element;
		}
		return bytes;
	}

	private byte[] toByteArray(long[] longs) { // TODO Test longs
		byte[] bytes = new byte[8 * longs.length];
		for (int index = 0; index < longs.length; index++) {
			long element = longs[index];
			int byteIndex = 8 * index;
			bytes[byteIndex] = (byte) (element >> 56);
			bytes[byteIndex + 1] = (byte) (element >> 48);
			bytes[byteIndex + 2] = (byte) (element >> 40);
			bytes[byteIndex + 3] = (byte) (element >> 32);
			bytes[byteIndex + 4] = (byte) (element >> 24);
			bytes[byteIndex + 5] = (byte) (element >> 16);
			bytes[byteIndex + 6] = (byte) (element >> 8);
			bytes[byteIndex + 7] = (byte) element;
		}
		return bytes;
	}

	@Override
	void readValue(Object value, int size, ReadJob read) throws IOException {
		if (value instanceof boolean[]) backToBooleanArray((boolean[]) value, read.input);
		else if (value instanceof byte[]) read.input.read((byte[]) value);
		else if (value instanceof short[]) backToShortArray((short[]) value, read.input);
		else if (value instanceof int[]) backToIntArray((int[]) value, read.input);
		else if (value instanceof long[]) backToLongArray((long[]) value, read.input);
		else throw new UnsupportedOperationException("Can't decode " + value.getClass() + " from bytes");
	}

	void backToBooleanArray(boolean[] booleans, BitInputStream input) throws IOException {
		int numBytes = booleans.length / 8;
		if (booleans.length % 8 != 0) numBytes += 1;
		byte[] bytes = new byte[numBytes];
		input.read(bytes);

		for (int boolIndex = 0; boolIndex < booleans.length; boolIndex++) {
			booleans[boolIndex] = (bytes[boolIndex >> 3] & (1 << (boolIndex & 7))) != 0;
		}
	}

	void backToShortArray(short[] shorts, BitInputStream input) throws IOException {
		byte[] bytes = new byte[2 * shorts.length];
		input.read(bytes);

		for (int shortIndex = 0; shortIndex < shorts.length; shortIndex++) {
			int byteIndex = 2 * shortIndex;
			int byte8 = toUnsignedInt(bytes[byteIndex]);
			int byte0 = toUnsignedInt(bytes[byteIndex + 1]);
			shorts[shortIndex] = (short) ((byte8 << 8) | byte0);
		}
	}

	void backToIntArray(int[] ints, BitInputStream input) throws IOException {
		byte[] bytes = new byte[4 * ints.length];
		input.read(bytes);

		for (int intIndex = 0; intIndex < ints.length; intIndex++) {
			int byteIndex = 4 * intIndex;
			int byte24 = toUnsignedInt(bytes[byteIndex]);
			int byte16 = toUnsignedInt(bytes[byteIndex + 1]);
			int byte8 = toUnsignedInt(bytes[byteIndex + 2]);
			int byte0 = toUnsignedInt(bytes[byteIndex + 3]);
			ints[intIndex] = (byte24 << 24) | (byte16 << 16) | (byte8 << 8) | byte0;
		}
	}

	void backToLongArray(long[] longs, BitInputStream input) throws IOException {
		byte[] bytes = new byte[8 * longs.length];
		input.read(bytes);

		for (int longIndex = 0; longIndex < longs.length; longIndex++) {
			int byteIndex = 8 * longIndex;
			long byte56 = toUnsignedInt(bytes[byteIndex]);
			long byte48 = toUnsignedInt(bytes[byteIndex + 1]);
			long byte40 = toUnsignedInt(bytes[byteIndex + 2]);
			long byte32 = toUnsignedInt(bytes[byteIndex + 3]);
			long byte24 = toUnsignedInt(bytes[byteIndex + 4]);
			long byte16 = toUnsignedInt(bytes[byteIndex + 5]);
			long byte8 = toUnsignedInt(bytes[byteIndex + 6]);
			long byte0 = toUnsignedInt(bytes[byteIndex + 7]);
			longs[longIndex] = (byte56 << 56) | (byte48 << 48) | (byte40 << 40) | (byte32 << 32) |
					(byte24 << 24) | (byte16 << 16) | (byte8 << 8) | byte0;
		}
	}

	@Override
	void setLegacyValue(ReadJob read, Object value, Consumer<Object> setValue) {
		if (field.type == value.getClass()) super.setLegacyValue(read, value, setValue);
		if (field.type == short[].class) super.setLegacyValue(read, legacyToShortArray(value), setValue);
		if (field.type == int[].class) super.setLegacyValue(read, legacyToIntArray(value), setValue);
		// TODO Other array types
	}

	private short[] legacyToShortArray(Object legacy) {
		short[] result = new short[Array.getLength(legacy)];
		for (int index = 0; index < result.length; index++) result[index] = ((Number) Array.get(legacy, index)).shortValue();
		return result;
	}

	private int[] legacyToIntArray(Object legacy) {
		int[] result = new int[Array.getLength(legacy)];
		for (int index = 0; index < result.length; index++) result[index] = ((Number) Array.get(legacy, index)).intValue();
		return result;
	}
}
