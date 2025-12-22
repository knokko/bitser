package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.exceptions.LegacyBitserException;
import com.github.knokko.bitser.exceptions.UnexpectedBitserException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.legacy.BackEnumName;
import com.github.knokko.bitser.legacy.BackEnumOrdinal;
import com.github.knokko.bitser.util.Recursor;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.function.Consumer;

import static com.github.knokko.bitser.IntegerBitser.*;
import static java.lang.Math.max;
import static java.lang.Math.min;

@BitStruct(backwardCompatible = false)
class EnumFieldWrapper extends BitFieldWrapper {

	@BitField
	private final BitEnum.Mode mode;

	@IntegerField(expectUniform = false, minValue = 0)
	private final int numEnumConstants;

	@IntegerField(expectUniform = false, minValue = 1)
	private final int minNameLength;

	@IntegerField(expectUniform = false, minValue = 1)
	private final int maxNameLength;

	EnumFieldWrapper(VirtualField field, BitEnum.Mode mode) {
		super(field);
		this.mode = mode;
		if (!field.type.isEnum()) throw new InvalidBitFieldException("BitEnum can only be used on enums, but got " + field);
		Enum<?>[] enumConstants = (Enum<?>[]) field.type.getEnumConstants();
		this.numEnumConstants = enumConstants.length;
		int minNameLength = Integer.MAX_VALUE;
		int maxNameLength = 0;
		for (Enum<?> constant : enumConstants) {
			int length = constant.name().length();
			minNameLength = min(length, minNameLength);
			maxNameLength = max(length, maxNameLength);
		}
		this.minNameLength = minNameLength;
		this.maxNameLength = maxNameLength;
	}

	@SuppressWarnings("unused")
	private EnumFieldWrapper() {
		super();
		this.mode = BitEnum.Mode.Name;
		this.numEnumConstants = 0;
		this.minNameLength = 0;
		this.maxNameLength = 0;
	}

	@Override
	void writeValue(Object value, Recursor<WriteContext, WriteInfo> recursor) {
		recursor.runFlat("enum-value", context -> {
			Enum<?> enumValue = (Enum<?>) value;
			if (mode == BitEnum.Mode.Name) {
				IntegerField.Properties lengthField = new IntegerField.Properties(
						minNameLength, maxNameLength, true, 0, new long[0]
				);
				StringBitser.encode(enumValue.name(), lengthField, context.output);
			} else if (mode == BitEnum.Mode.Ordinal) {
				int maxOrdinal = field.type.getEnumConstants().length - 1;
				context.output.prepareProperty("enum-ordinal", -1);
				encodeUniformInteger(enumValue.ordinal(), 0, maxOrdinal, context.output);
				context.output.finishProperty();
			} else throw new UnexpectedBitserException("Unknown enum mode: " + mode);
		});
	}

	@Override
	public void write(
			Serializer serializer, Object value,
			RecursionNode parentNode, String fieldName
	) throws Throwable {
		Enum<?> enumValue = (Enum<?>) value;
		if (mode == BitEnum.Mode.Name) {
			IntegerField.Properties lengthField = new IntegerField.Properties(
					minNameLength, maxNameLength, true, 0, new long[0]
			);
			StringBitser.encode(enumValue.name(), lengthField, serializer.output);
		} else if (mode == BitEnum.Mode.Ordinal) {
			int maxOrdinal = field.type.getEnumConstants().length - 1;
			serializer.output.prepareProperty("enum-ordinal", -1);
			encodeUniformInteger(enumValue.ordinal(), 0, maxOrdinal, serializer.output);
			serializer.output.finishProperty();
		} else throw new UnexpectedBitserException("Unknown enum mode: " + mode);
	}

	@Override
	public Object read(Deserializer deserializer, RecursionNode parentNode, String fieldName) throws Throwable {
		if (mode == BitEnum.Mode.Name) {
			IntegerField.Properties lengthField = new IntegerField.Properties(
					minNameLength, maxNameLength, true, 0, new long[0]
			);
			String name = StringBitser.decode(lengthField, deserializer.sizeLimit, deserializer.input);
			if (field.type == null) return name;
			try {
				return getConstantByName(name);
			} catch (NoSuchFieldException e) {
				throw new InvalidBitFieldException("Missing enum constant " + name + " in " + field.type);
			}
		}

		int ordinal;
		if (mode == BitEnum.Mode.Ordinal) {
			deserializer.input.prepareProperty("enum-ordinal", -1);
			ordinal = (int) decodeUniformInteger(0, numEnumConstants - 1, deserializer.input);
			deserializer.input.finishProperty();
		} else throw new UnexpectedBitserException("Unknown BitEnum mode: " + mode);

		if (field.type == null) return ordinal;

		Object[] constants = field.type.getEnumConstants();
		if (ordinal >= constants.length) {
			throw new InvalidBitFieldException("Missing enum ordinal " + ordinal + " in " + field.type);
		}
		return constants[ordinal];
	}

	@Override
	Object read(BackDeserializer deserializer, RecursionNode parentNode, String fieldName) throws Throwable {
		if (mode == BitEnum.Mode.Name) {
			IntegerField.Properties lengthField = new IntegerField.Properties(
					minNameLength, maxNameLength, true, 0, new long[0]
			);
			String name = StringBitser.decode(lengthField, deserializer.sizeLimit, deserializer.input);
			return new BackEnumName(name);
		}

		if (mode == BitEnum.Mode.Ordinal) {
			deserializer.input.prepareProperty("enum-ordinal", -1);
			int ordinal = (int) decodeUniformInteger(0, numEnumConstants - 1, deserializer.input);
			deserializer.input.finishProperty();
			return new BackEnumOrdinal(ordinal);
		} else throw new UnexpectedBitserException("Unknown BitEnum mode: " + mode);
	}

	@Override
	Object convert(BackDeserializer deserializer, Object legacyValue, RecursionNode parentNode, String fieldName) {
		if (legacyValue instanceof BackEnumName) {
			try {
				return getConstantByName(((BackEnumName) legacyValue).name);
			} catch (NoSuchFieldException fieldNoLongerExists) {
				throw new LegacyBitserException("Missing legacy " + legacyValue + " in " + field);
			}
		} else if (legacyValue instanceof BackEnumOrdinal) {
			int ordinal = ((BackEnumOrdinal) legacyValue).ordinal;
			Object[] constants = field.type.getEnumConstants();
			if (ordinal >= constants.length) {
				throw new LegacyBitserException("Missing legacy ordinal " + ordinal + " in " + field);
			}
			return constants[ordinal];
		} else {
			throw new LegacyBitserException("Can't convert from legacy " + legacyValue + " to " + field.type + " for field " + field);
		}
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
	void readValue(Recursor<ReadContext, ReadInfo> recursor, Consumer<Object> setValue) {
		recursor.runFlat("enum-value", context -> {
			if (mode == BitEnum.Mode.Name) {
				IntegerField.Properties lengthField = new IntegerField.Properties(
						minNameLength, maxNameLength, true, 0, new long[0]
				);
				String name = StringBitser.decode(lengthField, recursor.info.sizeLimit, context.input);
				if (field.type == null) {
					setValue.accept(name);
					return;
				}
				try {
					setValue.accept(getConstantByName(name));
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
				setValue.accept(ordinal);
				return;
			}

			Object[] constants = field.type.getEnumConstants();
			if (ordinal >= constants.length) {
				throw new InvalidBitFieldException("Missing enum ordinal " + ordinal + " in " + field.type);
			}
			setValue.accept(constants[ordinal]);
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
