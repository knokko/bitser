package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitEnum;
import com.github.knokko.bitser.backward.instance.LegacyCollectionInstance;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.serialize.ReadJob;
import com.github.knokko.bitser.serialize.WriteJob;
import com.github.knokko.bitser.util.VirtualField;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.Collection;

import static com.github.knokko.bitser.serialize.IntegerBitser.*;
import static com.github.knokko.bitser.serialize.IntegerBitser.decodeVariableInteger;
import static java.lang.Math.max;
import static java.lang.Math.min;

public abstract class AbstractCollectionFieldWrapper extends BitFieldWrapper {

	public static void writeElement(
			Object element, BitFieldWrapper wrapper, WriteJob write, String nullErrorMessage
	) throws IOException {
		if (wrapper.field.optional) {
			write.output.prepareProperty("optional", -1);
			write.output.write(element != null);
			write.output.finishProperty();
		}
		else if (element == null) throw new InvalidBitValueException(nullErrorMessage);
		if (element != null) {
			wrapper.writeValue(element, write);
			if (wrapper.field.referenceTargetLabel != null) {
				write.idMapper.maybeEncodeUnstableId(wrapper.field.referenceTargetLabel, element, write.output);
			}
		}
	}

	static Object constructCollectionWithSize(Class<?> fieldType, int size) {
		try {
			return fieldType.getConstructor(int.class).newInstance(size);
		} catch (NoSuchMethodException noIntConstructor) {
			try {
				return fieldType.getConstructor().newInstance();
			} catch (Exception unexpected) {
				throw new RuntimeException(unexpected);
			}
		} catch (Exception unexpected) {
			throw new RuntimeException(unexpected);
		}
	}

	@BitField
	private final IntegerField.Properties sizeField;

	@BitField(optional = true)
	private final ArrayType arrayType;

	AbstractCollectionFieldWrapper(VirtualField field, IntegerField sizeField) {
		super(field);
		if (sizeField.minValue() > Integer.MAX_VALUE || sizeField.maxValue() < 0) throw new IllegalArgumentException();
		if (!field.type.isArray() && (field.type.isInterface() || Modifier.isAbstract(field.type.getModifiers()))) {
			throw new InvalidBitFieldException("Field type must not be abstract or an interface: " + field);
		}
		this.sizeField = new IntegerField.Properties(sizeField);
		this.arrayType = determineArrayType();
	}

	AbstractCollectionFieldWrapper() {
		super();
		this.sizeField = new IntegerField.Properties();
		this.arrayType = null;
	}

	abstract ArrayType determineArrayType();

	@Override
	void writeValue(Object value, WriteJob write) throws IOException {
		int size = getCollectionSize(value);
		write.output.prepareProperty("size", -1);
		if (sizeField.expectUniform) encodeUniformInteger(size, getMinSize(), getMaxSize(), write.output);
		else encodeVariableInteger(size, getMinSize(), getMaxSize(), write.output);
		write.output.finishProperty();

		writeElements(value, size, write);
	}

	@Override
	void readValue(ReadJob read, ValueConsumer setValue) throws IOException {
		int size;
		if (sizeField.expectUniform) size = (int) decodeUniformInteger(getMinSize(), getMaxSize(), read.input);
		else size = (int) decodeVariableInteger(getMinSize(), getMaxSize(), read.input);

		Object value = constructCollectionWithSize(size);
		readElements(value, size, read);

		if (read.backwardCompatible) setValue.consume(new LegacyCollectionInstance(value));
		else setValue.consume(value);
	}

	@Override
	public void fixLegacyTypes(ReadJob read, Object value) {
		if (value == null) return;
		LegacyCollectionInstance legacyInstance = (LegacyCollectionInstance) value;
		legacyInstance.newCollection = constructCollectionWithSize(Array.getLength(legacyInstance.legacyArray));
		if (field.referenceTargetLabel != null) {
			read.idLoader.replace(field.referenceTargetLabel, legacyInstance, legacyInstance.newCollection);
		}
	}

	abstract void writeElements(Object value, int size, WriteJob write) throws IOException;

	abstract void readElements(Object value, int size, ReadJob read) throws IOException;

	private int getCollectionSize(Object object) {
		if (object instanceof Collection<?>) return ((Collection<?>) object).size();
		return Array.getLength(object);
	}

	private Object constructCollectionWithSize(int size) {
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

	private int getMinSize() {
		return (int) max(0, sizeField.minValue);
	}

	private int getMaxSize() {
		return (int) min(Integer.MAX_VALUE, sizeField.maxValue);
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
