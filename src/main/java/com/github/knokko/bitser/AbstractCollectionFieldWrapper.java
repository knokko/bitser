package com.github.knokko.bitser;

import com.github.knokko.bitser.legacy.LegacyCollectionInstance;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.util.JobOutput;
import com.github.knokko.bitser.util.Recursor;

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.function.Consumer;

import static com.github.knokko.bitser.IntegerBitser.*;
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
	private final IntegerField.Properties sizeField;

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

	@Override
	void writeValue(Object value, Recursor<WriteContext, WriteInfo> recursor) {
		int size = getCollectionSize(value);
		recursor.runFlat("size", context -> {
			if (context.integerDistribution != null) {
				context.integerDistribution.insert(field + " collection size", (long) size, sizeField);
				context.integerDistribution.insert("collection size", (long) size, sizeField);
			}
			context.output.prepareProperty("size", -1);
			encodeInteger(size, sizeField, context.output);
			context.output.finishProperty();
		});

		if (size > 0) writeElements(value, size, recursor);
	}

	@Override
	void readValue(Recursor<ReadContext, ReadInfo> recursor, Consumer<Object> setValue) {
		JobOutput<Integer> size = recursor.computeFlat("size", context ->
				decodeLength(sizeField, recursor.info.sizeLimit, "collection size", context.input)
		);

		recursor.runNested("elements", nested -> {
			Object value = constructCollectionWithSize(size.get());
			if (size.get() > 0) readElements(value, size.get(), nested);

			nested.runFlat("add-elements", context -> {
				if (nested.info.backwardCompatible) setValue.accept(new LegacyCollectionInstance(value));
				else setValue.accept(value);
			});
		});
	}

	@Override
	void fixLegacyTypes(Recursor<ReadContext, ReadInfo> recursor, Object value) {
		if (value == null) return;
		LegacyCollectionInstance legacyInstance = (LegacyCollectionInstance) value;
		legacyInstance.newCollection = constructCollectionWithSize(
				Array.getLength(legacyInstance.legacyArray)
		);
		if (field.referenceTargetLabel != null) {
			recursor.runFlat("referenceTargetLabel", context ->
					context.idLoader.replace(field.referenceTargetLabel, legacyInstance, legacyInstance.newCollection)
			);
		}
	}

	abstract void writeElements(Object value, int size, Recursor<WriteContext, WriteInfo> recursor) ;

	abstract void readElements(Object value, int size, Recursor<ReadContext, ReadInfo> recursor) ;

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
