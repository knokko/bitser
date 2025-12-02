package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.backward.instance.LegacyCollectionInstance;
import com.github.knokko.bitser.context.ReadContext;
import com.github.knokko.bitser.context.ReadInfo;
import com.github.knokko.bitser.context.WriteContext;
import com.github.knokko.bitser.context.WriteInfo;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.UnexpectedBitserException;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.serialize.BitserCache;
import com.github.knokko.bitser.util.Recursor;
import com.github.knokko.bitser.util.VirtualField;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Objects;
import java.util.function.Consumer;

import static java.lang.Byte.toUnsignedInt;
import static java.lang.Double.doubleToRawLongBits;
import static java.lang.Double.longBitsToDouble;
import static java.lang.Float.floatToRawIntBits;
import static java.lang.Float.intBitsToFloat;

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
		throw new InvalidBitFieldException("Unexpected write-as-bytes field type " + field.type);
	}

	@Override
	void writeElements(Object value, int size, Recursor<WriteContext, WriteInfo> recursor) {
		recursor.runFlat("byte-values", context -> {
			context.output.prepareProperty("byte-values", -1);
			if (value instanceof boolean[]) context.output.write(toByteArray((boolean[]) value));
			else if (value instanceof byte[]) context.output.write((byte[]) value);
			else if (value instanceof short[]) context.output.write(toByteArray((short[]) value));
			else if (value instanceof char[]) context.output.write(toByteArray((char[]) value));
			else if (value instanceof int[]) context.output.write(toByteArray((int[]) value));
			else if (value instanceof float[]) context.output.write(toByteArray((float[]) value));
			else if (value instanceof long[]) context.output.write(toByteArray((long[]) value));
			else if (value instanceof double[]) context.output.write(toByteArray((double[]) value));
			else throw new UnexpectedBitserException("Can't encode " + value.getClass() + " as bytes");
			context.output.finishProperty();
		});
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

	private byte[] toByteArray(char[] chars) {
		byte[] bytes = new byte[2 * chars.length];
		for (int index = 0; index < chars.length; index++) {
			char element = chars[index];
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

	private byte[] toByteArray(float[] floats) {
		byte[] bytes = new byte[4 * floats.length];
		for (int index = 0; index < floats.length; index++) {
			int element = floatToRawIntBits(floats[index]);
			int byteIndex = 4 * index;
			bytes[byteIndex] = (byte) (element >> 24);
			bytes[byteIndex + 1] = (byte) (element >> 16);
			bytes[byteIndex + 2] = (byte) (element >> 8);
			bytes[byteIndex + 3] = (byte) element;
		}
		return bytes;
	}

	private byte[] toByteArray(long[] longs) {
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

	private byte[] toByteArray(double[] doubles) {
		byte[] bytes = new byte[8 * doubles.length];
		for (int index = 0; index < doubles.length; index++) {
			long element = doubleToRawLongBits(doubles[index]);
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
	void readElements(Object value, int size, Recursor<ReadContext, ReadInfo> recursor) {
		recursor.runFlat("bytes", context -> {
			if (value instanceof boolean[]) backToBooleanArray((boolean[]) value, context.input);
			else if (value instanceof byte[]) context.input.read((byte[]) value);
			else if (value instanceof short[]) backToShortArray((short[]) value, context.input);
			else if (value instanceof char[]) backToCharArray((char[]) value, context.input);
			else if (value instanceof int[]) backToIntArray((int[]) value, context.input);
			else if (value instanceof float[]) backToFloatArray((float[]) value, context.input);
			else if (value instanceof long[]) backToLongArray((long[]) value, context.input);
			else if (value instanceof double[]) backToDoubleArray((double[]) value, context.input);
			else throw new InvalidBitFieldException("Can't decode " + value.getClass() + " from bytes");
		});
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

	void backToCharArray(char[] chars, BitInputStream input) throws IOException {
		byte[] bytes = new byte[2 * chars.length];
		input.read(bytes);

		for (int charIndex = 0; charIndex < chars.length; charIndex++) {
			int byteIndex = 2 * charIndex;
			int byte8 = toUnsignedInt(bytes[byteIndex]);
			int byte0 = toUnsignedInt(bytes[byteIndex + 1]);
			chars[charIndex] = (char) ((byte8 << 8) | byte0);
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

	void backToFloatArray(float[] floats, BitInputStream input) throws IOException {
		byte[] bytes = new byte[4 * floats.length];
		input.read(bytes);

		for (int floatIndex = 0; floatIndex < floats.length; floatIndex++) {
			int byteIndex = 4 * floatIndex;
			int byte24 = toUnsignedInt(bytes[byteIndex]);
			int byte16 = toUnsignedInt(bytes[byteIndex + 1]);
			int byte8 = toUnsignedInt(bytes[byteIndex + 2]);
			int byte0 = toUnsignedInt(bytes[byteIndex + 3]);
			floats[floatIndex] = intBitsToFloat((byte24 << 24) | (byte16 << 16) | (byte8 << 8) | byte0);
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

	void backToDoubleArray(double[] doubles, BitInputStream input) throws IOException {
		byte[] bytes = new byte[8 * doubles.length];
		input.read(bytes);

		for (int doubleIndex = 0; doubleIndex < doubles.length; doubleIndex++) {
			int byteIndex = 8 * doubleIndex;
			long byte56 = toUnsignedInt(bytes[byteIndex]);
			long byte48 = toUnsignedInt(bytes[byteIndex + 1]);
			long byte40 = toUnsignedInt(bytes[byteIndex + 2]);
			long byte32 = toUnsignedInt(bytes[byteIndex + 3]);
			long byte24 = toUnsignedInt(bytes[byteIndex + 4]);
			long byte16 = toUnsignedInt(bytes[byteIndex + 5]);
			long byte8 = toUnsignedInt(bytes[byteIndex + 6]);
			long byte0 = toUnsignedInt(bytes[byteIndex + 7]);
			doubles[doubleIndex] = longBitsToDouble((byte56 << 56) | (byte48 << 48) | (byte40 << 40) | (byte32 << 32) |
					(byte24 << 24) | (byte16 << 16) | (byte8 << 8) | byte0);
		}
	}

	@Override
	void setLegacyValue(Recursor<ReadContext, ReadInfo> recursor, Object rawLegacyInstance, Consumer<Object> setValue) {
		if (rawLegacyInstance == null) {
			super.setLegacyValue(recursor, null, setValue);
			return;
		}

		LegacyCollectionInstance legacyInstance = (LegacyCollectionInstance) rawLegacyInstance;
		if (field.type == legacyInstance.legacyArray.getClass()) {
			super.setLegacyValue(recursor, legacyInstance.legacyArray, setValue);
			return;
		}

		int size = Array.getLength(legacyInstance.legacyArray);
		for (int index = 0; index < size; index++) {
			Object legacyValue = Array.get(legacyInstance.legacyArray, index);
			setFromLegacyValue(legacyInstance.newCollection, index, legacyValue);
		}

		setValue.accept(legacyInstance.newCollection);
	}

	private void setFromLegacyValue(Object newArray, int index, Object legacyValue) {
		if (legacyValue == null) {
			Array.set(newArray, index, null);
			return;
		}
		Object newNumber = convertLegacyNumber(legacyValue);
		Array.set(newArray, index, newNumber);
	}

	private Object convertLegacyNumber(Object legacyValue) {
		if (legacyValue instanceof Boolean) return legacyValue;
		Number legacyNumber = legacyValue instanceof Character ? ((int) ((char) legacyValue)) : (Number) legacyValue;
		if (field.type == byte[].class) return legacyNumber.byteValue();
		if (field.type == short[].class) return legacyNumber.shortValue();
		if (field.type == char[].class) return (char) legacyNumber.intValue();
		if (field.type == int[].class) return legacyNumber.intValue();
		if (field.type == float[].class) return legacyNumber.floatValue();
		if (field.type == long[].class) return legacyNumber.longValue();
		if (field.type == double[].class) return legacyNumber.doubleValue();
		throw new UnexpectedBitserException("Unexpected write-as-bytes type " + field.type);
	}

	@Override
	boolean deepEquals(Object a, Object b, BitserCache cache) {
		return Objects.deepEquals(a, b);
	}

	@Override
	int hashCode(Object value, BitserCache cache) {
		if (value == null) return -9;

		int code = 9;
		int length = Array.getLength(value);
		for (int index = 0; index < length; index++) {
			code = 37 * code + Array.get(value, index).hashCode();
		}
		return code;
	}
}
