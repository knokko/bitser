package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitEnum;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.BitserCache;
import com.github.knokko.bitser.util.ReferenceIdLoader;
import com.github.knokko.bitser.util.ReferenceIdMapper;
import com.github.knokko.bitser.util.VirtualField;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static com.github.knokko.bitser.serialize.IntegerBitser.*;

class EnumFieldWrapper extends BitFieldWrapper {

	private final BitEnum bitEnum;

	EnumFieldWrapper(VirtualField field, BitEnum bitEnum) {
		super(field);
		this.bitEnum = Objects.requireNonNull(bitEnum);
		if (!field.type.isEnum()) throw new InvalidBitFieldException("BitEnum can only be used on enums, but got " + field);
	}

	@Override
	void writeValue(
			Object value, BitOutputStream output, BitserCache cache, ReferenceIdMapper idMapper
	) throws IOException {
		Enum<?> enumValue = (Enum<?>) value;
		if (bitEnum.mode() == BitEnum.Mode.Name) {
			byte[] bytes = enumValue.name().getBytes(StandardCharsets.UTF_8);
			encodeVariableInteger(bytes.length, 1, Integer.MAX_VALUE, output);
			for (byte stringByte : bytes) encodeUniformInteger(stringByte, Byte.MIN_VALUE, Byte.MAX_VALUE, output);
		} else if (bitEnum.mode() == BitEnum.Mode.UniformOrdinal) {
			int maxOrdinal = field.type.getEnumConstants().length - 1;
			encodeUniformInteger(enumValue.ordinal(), 0, maxOrdinal, output);
		} else if (bitEnum.mode() == BitEnum.Mode.VariableIntOrdinal) {
			encodeVariableInteger(enumValue.ordinal(), 0, 65535, output);
		} else throw new Error("Unknown enum mode: " + bitEnum.mode());
	}

	@Override
	void readValue(
			BitInputStream input, BitserCache cache, ReferenceIdLoader idLoader, ValueConsumer setValue
	) throws IOException {
		if (bitEnum.mode() == BitEnum.Mode.Name) {
			int numBytes = (int) decodeVariableInteger(1, Integer.MAX_VALUE, input);
			byte[] stringBytes = new byte[numBytes];
			for (int index = 0; index < numBytes; index++) {
				stringBytes[index] = (byte) decodeUniformInteger(Byte.MIN_VALUE, Byte.MAX_VALUE, input);
			}
			String name = new String(stringBytes, StandardCharsets.UTF_8);
			try {
				setValue.consume(field.type.getDeclaredField(name).get(null));
				return;
			} catch (NoSuchFieldException e) {
				throw new InvalidBitFieldException("Missing enum constant " + name + " in " + field.type);
			} catch (IllegalAccessException e) {
				throw new Error(e);
			}
		}

		int ordinal;
		if (bitEnum.mode() == BitEnum.Mode.UniformOrdinal) {
			int maxOrdinal = field.type.getEnumConstants().length - 1;
			ordinal = (int) decodeUniformInteger(0, maxOrdinal, input);
		} else if (bitEnum.mode() == BitEnum.Mode.VariableIntOrdinal) {
			ordinal = (int) decodeVariableInteger(0, 65535, input);
		} else throw new Error("Unknown BitEnum mode: " + bitEnum.mode());

		Object[] constants = field.type.getEnumConstants();
		if (ordinal >= constants.length) {
			throw new InvalidBitFieldException("Missing enum ordinal " + ordinal + " in " + field.type);
		}
		setValue.consume(constants[ordinal]);
	}
}
