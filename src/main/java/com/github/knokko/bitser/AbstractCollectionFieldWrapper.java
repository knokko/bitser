package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.IntegerField;

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;

import static java.lang.Math.max;
import static java.lang.Math.min;

abstract class AbstractCollectionFieldWrapper extends BitFieldWrapper {

	static Object constructCollectionWithSize(Class<?> fieldType, int size) {
		try {
			return fieldType.getConstructor(int.class).newInstance(size);
		} catch (NoSuchMethodException noIntConstructor) {
			try {
				return fieldType.getConstructor().newInstance();
			} catch (Exception unexpected) {
				throw new InvalidBitFieldException(
						"Failed to find constructor of " + fieldType + ": bitser requires either a constructor with " +
								"no arguments, or a constructor with exactly 1 argument whose type is int");
			}
		} catch (Exception constructorFailed) {
			throw new InvalidBitFieldException("Failed to instantiate " + fieldType + ": " + constructorFailed.getMessage());
		}
	}

	@BitField
	final IntegerField.Properties sizeField;

	@BitField(optional = true)
	private final ArrayType arrayType;

	AbstractCollectionFieldWrapper(VirtualField field, IntegerField sizeField) {
		super(field);
		if (sizeField.minValue() > Integer.MAX_VALUE || sizeField.maxValue() < 0) {
			throw new InvalidBitFieldException("Invalid size field");
		}
		if (!field.type.isArray() && (field.type.isInterface() || Modifier.isAbstract(field.type.getModifiers()))) {
			throw new InvalidBitFieldException("Field type must not be abstract or an interface: " + field);
		}
		this.sizeField = new IntegerField.Properties(
				max(0, sizeField.minValue()), min(Integer.MAX_VALUE, sizeField.maxValue()),
				sizeField.expectUniform(), sizeField.digitSize(), sizeField.commonValues()
		);
		this.arrayType = determineArrayType();
	}

	AbstractCollectionFieldWrapper() {
		super();
		this.sizeField = new IntegerField.Properties();
		this.arrayType = null;
	}

	abstract ArrayType determineArrayType();

	protected Object constructCollectionWithSize(int size) {
		if (field.type == null) {
			if (arrayType == null) return new Object[size];
			switch (arrayType) {
				case BOOLEAN: return new boolean[size];
				case BYTE: return new byte[size];
				case SHORT: return new short[size];
				case CHAR: return new char[size];
				case INT: return new int[size];
				case FLOAT: return new float[size];
				case LONG: return new long[size];
				case DOUBLE: return new double[size];
			}
		}
		if (field.type.isArray()) {
			return Array.newInstance(field.type.getComponentType(), size);
		} else {
			return constructCollectionWithSize(field.type, size);
		}
	}

	@BitEnum(mode = BitEnum.Mode.Ordinal)
	enum ArrayType {
		BOOLEAN,
		BYTE,
		SHORT,
		CHAR,
		INT,
		FLOAT,
		LONG,
		DOUBLE
	}
}
