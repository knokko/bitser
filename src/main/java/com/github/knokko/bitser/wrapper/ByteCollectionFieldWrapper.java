package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.BitserCache;
import com.github.knokko.bitser.util.ReferenceIdLoader;
import com.github.knokko.bitser.util.ReferenceIdMapper;
import com.github.knokko.bitser.util.VirtualField;

import java.io.IOException;

import static java.lang.Byte.toUnsignedInt;

class ByteCollectionFieldWrapper extends AbstractCollectionFieldWrapper {

	ByteCollectionFieldWrapper(VirtualField field, IntegerField sizeField) {
		super(field, sizeField);
	}

	@Override
	void writeValue(
			Object value, int size, BitOutputStream output, BitserCache cache, ReferenceIdMapper idMapper
	) throws IOException {
		if (value instanceof boolean[]) output.write(toByteArray((boolean[]) value));
		else if (value instanceof byte[]) output.write((byte[]) value);
		else if (value instanceof int[]) output.write(toByteArray((int[]) value));
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

	@Override
	void readValue(Object value, int size, BitInputStream input, BitserCache cache, ReferenceIdLoader idLoader) throws IOException {
		if (value instanceof boolean[]) backToBooleanArray((boolean[]) value, input);
		else if (value instanceof byte[]) input.read((byte[]) value);
		else if (value instanceof int[]) backToIntArray((int[]) value, input);
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
}
