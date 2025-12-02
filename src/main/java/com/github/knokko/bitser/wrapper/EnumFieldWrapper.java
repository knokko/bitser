package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitEnum;
import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.context.ReadContext;
import com.github.knokko.bitser.context.ReadInfo;
import com.github.knokko.bitser.context.WriteContext;
import com.github.knokko.bitser.context.WriteInfo;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.exceptions.UnexpectedBitserException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.serialize.BitserCache;
import com.github.knokko.bitser.util.Recursor;
import com.github.knokko.bitser.util.VirtualField;

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
	void writeValue(Object value, Recursor<WriteContext, WriteInfo> recursor) {
		recursor.runFlat("enum-value", context -> {
			Enum<?> enumValue = (Enum<?>) value;
			if (mode == BitEnum.Mode.Name) {
				byte[] bytes = enumValue.name().getBytes(StandardCharsets.UTF_8);
				context.output.prepareProperty("enum-name-length", -1);
				encodeVariableInteger(bytes.length, 1, Integer.MAX_VALUE, context.output);
				context.output.finishProperty();

				int counter = 0;
				for (byte stringByte : bytes) {
					context.output.prepareProperty("enum-name-char", counter++);
					encodeUniformInteger(stringByte, Byte.MIN_VALUE, Byte.MAX_VALUE, context.output);
					context.output.finishProperty();
				}
			} else if (mode == BitEnum.Mode.Ordinal) {
				int maxOrdinal = field.type.getEnumConstants().length - 1;
				context.output.prepareProperty("enum-ordinal", -1);
				encodeUniformInteger(enumValue.ordinal(), 0, maxOrdinal, context.output);
				context.output.finishProperty();
			} else throw new UnexpectedBitserException("Unknown enum mode: " + mode);
		});
	}

	private Object getConstantByName(String name) throws NoSuchFieldException {
		try {
			Field constantField = field.type.getDeclaredField(name);
			if (!Modifier.isPublic(field.type.getModifiers())) constantField.setAccessible(true);
			return constantField.get(null);
		} catch (IllegalAccessException e) {
			throw new UnexpectedBitserException("Failed to access enum constant " + name + " of " + field.type);
		}
	}

	@Override
	void readValue(Recursor<ReadContext, ReadInfo> recursor, ValueConsumer setValue) {
		recursor.runFlat("enum-value", context -> {
			if (mode == BitEnum.Mode.Name) {
				int numBytes = (int) decodeVariableInteger(1, Integer.MAX_VALUE, context.input);
				byte[] stringBytes = new byte[numBytes];
				for (int index = 0; index < numBytes; index++) {
					stringBytes[index] = (byte) decodeUniformInteger(Byte.MIN_VALUE, Byte.MAX_VALUE, context.input);
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
				ordinal = (int) decodeUniformInteger(0, numEnumConstants - 1, context.input);
			} else throw new UnexpectedBitserException("Unknown BitEnum mode: " + mode);

			if (field.type == null) {
				setValue.consume(ordinal);
				return;
			}

			Object[] constants = field.type.getEnumConstants();
			if (ordinal >= constants.length) {
				throw new InvalidBitFieldException("Missing enum ordinal " + ordinal + " in " + field.type);
			}
			setValue.consume(constants[ordinal]);
		});
	}

	@Override
	void setLegacyValue(Recursor<ReadContext, ReadInfo> recursor, Object legacyValue, Consumer<Object> setValue) {
		if (legacyValue instanceof String) {
			try {
				super.setLegacyValue(recursor, getConstantByName((String) legacyValue), setValue);
			} catch (NoSuchFieldException e) {
				throw new InvalidBitValueException("Missing legacy enum constant " + legacyValue + " in " + field);
			}
		} else if (legacyValue instanceof Integer) {
			int ordinal = (Integer) legacyValue;
			Object[] constants = field.type.getEnumConstants();
			if (ordinal >= constants.length) {
				throw new InvalidBitValueException("Missing legacy ordinal " + ordinal + " in " + field);
			}
			super.setLegacyValue(recursor, constants[ordinal], setValue);
		} else super.setLegacyValue(recursor, legacyValue, setValue);
	}

	@Override
	int hashCode(Object value, BitserCache cache) {
		Enum<?> enumValue = (Enum<?>) value;
		if (enumValue == null) return -192347;
		if (mode == BitEnum.Mode.Ordinal) return 191 * enumValue.ordinal();
		if (mode == BitEnum.Mode.Name) return -13 * enumValue.name().hashCode();
		throw new UnexpectedBitserException("Unknown Mode " + mode);
	}
}
