package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitEnum;
import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.serialize.ReadJob;
import com.github.knokko.bitser.serialize.WriteJob;
import com.github.knokko.bitser.util.VirtualField;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import static com.github.knokko.bitser.serialize.IntegerBitser.*;

@BitStruct(backwardCompatible = false)
class EnumFieldWrapper extends BitFieldWrapper {

	@BitField
	private final BitEnum.Mode mode;

	@IntegerField(expectUniform = false, minValue = 0)
	private final int numEnumConstants;

	EnumFieldWrapper(VirtualField field, BitEnum.Mode mode) {
		super(field);
		this.mode = mode;
		if (!field.type.isEnum()) throw new InvalidBitFieldException("BitEnum can only be used on enums, but got " + field);
		this.numEnumConstants = field.type.getEnumConstants().length;
	}

	@SuppressWarnings("unused")
	private EnumFieldWrapper() {
		super();
		this.mode = BitEnum.Mode.Name;
		this.numEnumConstants = 0;
	}

	@Override
	void writeValue(Object value, WriteJob write) throws IOException {
		Enum<?> enumValue = (Enum<?>) value;
		if (mode == BitEnum.Mode.Name) {
			byte[] bytes = enumValue.name().getBytes(StandardCharsets.UTF_8);
			encodeVariableInteger(bytes.length, 1, Integer.MAX_VALUE, write.output);
			for (byte stringByte : bytes) encodeUniformInteger(stringByte, Byte.MIN_VALUE, Byte.MAX_VALUE, write.output);
		} else if (mode == BitEnum.Mode.Ordinal) {
			int maxOrdinal = field.type.getEnumConstants().length - 1;
			encodeUniformInteger(enumValue.ordinal(), 0, maxOrdinal, write.output);
		} else throw new Error("Unknown enum mode: " + mode);
	}

	private Object getConstantByName(String name) throws NoSuchFieldException {
		try {
			Field constantField = field.type.getDeclaredField(name);
			if (!Modifier.isPublic(field.type.getModifiers())) constantField.setAccessible(true);
			return constantField.get(null);
		} catch (IllegalAccessException e) {
			throw new Error(e);
		}
	}

	@Override
	void readValue(ReadJob read, ValueConsumer setValue) throws IOException {
		if (mode == BitEnum.Mode.Name) {
			int numBytes = (int) decodeVariableInteger(1, Integer.MAX_VALUE, read.input);
			byte[] stringBytes = new byte[numBytes];
			for (int index = 0; index < numBytes; index++) {
				stringBytes[index] = (byte) decodeUniformInteger(Byte.MIN_VALUE, Byte.MAX_VALUE, read.input);
			}
			String name = new String(stringBytes, StandardCharsets.UTF_8);
			if (field.type == null) {
				setValue.consume(name);
				return;
			}
			try {
				setValue.consume(getConstantByName(name));
				return;
			} catch (NoSuchFieldException e) {
				throw new InvalidBitFieldException("Missing enum constant " + name + " in " + field.type);
			}
		}

		int ordinal;
		if (mode == BitEnum.Mode.Ordinal) {
			ordinal = (int) decodeUniformInteger(0, numEnumConstants - 1, read.input);
		} else throw new Error("Unknown BitEnum mode: " + mode);

		if (field.type == null) {
			setValue.consume(ordinal);
			return;
		}

		Object[] constants = field.type.getEnumConstants();
		if (ordinal >= constants.length) {
			throw new InvalidBitFieldException("Missing enum ordinal " + ordinal + " in " + field.type);
		}
		setValue.consume(constants[ordinal]);
	}

	@Override
	void setLegacyValue(ReadJob read, Object legacyValue, Consumer<Object> setValue) {
		if (legacyValue instanceof String) {
			try {
				super.setLegacyValue(read, getConstantByName((String) legacyValue), setValue);
			} catch (NoSuchFieldException e) {
				throw new InvalidBitValueException("Missing legacy enum constant " + legacyValue + " in " + field);
			}
		} else if (legacyValue instanceof Integer) {
			int ordinal = (Integer) legacyValue;
			Object[] constants = field.type.getEnumConstants();
			if (ordinal >= constants.length) {
				throw new InvalidBitValueException("Missing legacy ordinal " + ordinal + " in " + field);
			}
			super.setLegacyValue(read, constants[ordinal], setValue);
		} else super.setLegacyValue(read, legacyValue, setValue);
	}
}
