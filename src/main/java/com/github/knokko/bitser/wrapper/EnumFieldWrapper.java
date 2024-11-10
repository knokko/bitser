package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitEnum;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.BitserCache;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static com.github.knokko.bitser.serialize.IntegerBitser.*;

class EnumFieldWrapper extends BitFieldWrapper {

	private final BitEnum bitEnum;
	private final Class<?> enumClass;

	EnumFieldWrapper(BitField.Properties properties, Field classField, BitEnum bitEnum, Class<?> enumClass) {
		super(properties, classField);
		this.bitEnum = Objects.requireNonNull(bitEnum);
		if (!enumClass.isEnum()) throw new InvalidBitFieldException("BitEnum can only be used on enums, but got " + enumClass);
		this.enumClass = enumClass;
	}

	@Override
	void writeValue(Object value, BitOutputStream output, BitserCache cache) throws IOException, IllegalAccessException {
		Enum<?> enumValue = (Enum<?>) value;
		if (bitEnum.mode() == BitEnum.Mode.Name) {
			byte[] bytes = enumValue.name().getBytes(StandardCharsets.UTF_8);
			encodeVariableInteger(bytes.length, 1, Integer.MAX_VALUE, output);
			for (byte stringByte : bytes) encodeUniformInteger(stringByte, Byte.MIN_VALUE, Byte.MAX_VALUE, output);
		} else if (bitEnum.mode() == BitEnum.Mode.UniformOrdinal) {
			int maxOrdinal = enumClass.getEnumConstants().length - 1;
			encodeUniformInteger(enumValue.ordinal(), 0, maxOrdinal, output);
		} else if (bitEnum.mode() == BitEnum.Mode.VariableIntOrdinal) {
			encodeVariableInteger(enumValue.ordinal(), 0, 65535, output);
		} else throw new Error("Unknown enum mode: " + bitEnum.mode());
	}

	@Override
	Object readValue(BitInputStream input, BitserCache cache) throws IOException, IllegalAccessException {
		if (bitEnum.mode() == BitEnum.Mode.Name) {
			int numBytes = (int) decodeVariableInteger(1, Integer.MAX_VALUE, input);
			byte[] stringBytes = new byte[numBytes];
			for (int index = 0; index < numBytes; index++) {
				stringBytes[index] = (byte) decodeUniformInteger(Byte.MIN_VALUE, Byte.MAX_VALUE, input);
			}
			String name = new String(stringBytes, StandardCharsets.UTF_8);
			try {
				return enumClass.getDeclaredField(name).get(null);
			} catch (NoSuchFieldException e) {
				throw new InvalidBitFieldException("Missing enum constant " + name + " in " + enumClass);
			}
		}

		int ordinal;
		if (bitEnum.mode() == BitEnum.Mode.UniformOrdinal) {
			int maxOrdinal = enumClass.getEnumConstants().length - 1;
			ordinal = (int) decodeUniformInteger(0, maxOrdinal, input);
		} else if (bitEnum.mode() == BitEnum.Mode.VariableIntOrdinal) {
			ordinal = (int) decodeVariableInteger(0, 65535, input);
		} else throw new Error("Unknown BitEnum mode: " + bitEnum.mode());

		Object[] constants = enumClass.getEnumConstants();
		if (ordinal >= constants.length) {
			throw new InvalidBitFieldException("Missing enum ordinal " + ordinal + " in " + enumClass);
		}
		return constants[ordinal];
	}
}
