package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitEnum;
import com.github.knokko.bitser.backward.instance.LegacyCollectionInstance;
import com.github.knokko.bitser.context.ReadContext;
import com.github.knokko.bitser.context.ReadInfo;
import com.github.knokko.bitser.context.WriteContext;
import com.github.knokko.bitser.context.WriteInfo;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.util.JobOutput;
import com.github.knokko.bitser.util.Recursor;
import com.github.knokko.bitser.util.VirtualField;

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.Collection;

import static com.github.knokko.bitser.serialize.IntegerBitser.*;
import static com.github.knokko.bitser.serialize.IntegerBitser.decodeVariableInteger;
import static java.lang.Math.max;
import static java.lang.Math.min;

public abstract class AbstractCollectionFieldWrapper extends BitFieldWrapper {

	public static void writeElement(
			Object element, BitFieldWrapper wrapper, Recursor<WriteContext, WriteInfo> recursor, String nullErrorMessage
	) {
		if (wrapper.field.optional) {
			recursor.runFlat("optional", context -> {
				context.output.prepareProperty("optional", -1);
				context.output.write(element != null);
				context.output.finishProperty();
			});
		}
		else if (element == null) throw new InvalidBitValueException(nullErrorMessage);
		if (element != null) {
			wrapper.writeValue(element, recursor);
			if (wrapper.field.referenceTargetLabel != null) {
				recursor.runFlat("referenceTargetLabel", context ->
						context.idMapper.maybeEncodeUnstableId(wrapper.field.referenceTargetLabel, element, context.output)
				);
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
	void writeValue(Object value, Recursor<WriteContext, WriteInfo> recursor) {
		int size = getCollectionSize(value);
		recursor.runFlat("size", context -> {
			context.output.prepareProperty("size", -1);
			if (sizeField.expectUniform) encodeUniformInteger(size, getMinSize(), getMaxSize(), context.output);
			else encodeVariableInteger(size, getMinSize(), getMaxSize(), context.output);
			context.output.finishProperty();
		});

		writeElements(value, size, recursor);
	}

	@Override
	void readValue(Recursor<ReadContext, ReadInfo> recursor, ValueConsumer setValue) {
		JobOutput<Integer> size = recursor.computeFlat("size", context -> {
			if (sizeField.expectUniform) return (int) decodeUniformInteger(getMinSize(), getMaxSize(), context.input);
			else return (int) decodeVariableInteger(getMinSize(), getMaxSize(), context.input);
		});

		recursor.runNested("elements", nested -> {
			Object value = constructCollectionWithSize(size.get());
			readElements(value, size.get(), nested);

			nested.runFlat("add-elements", context -> {
				if (nested.info.backwardCompatible) setValue.consume(new LegacyCollectionInstance(value));
				else setValue.consume(value);
			});
		});
	}

	@Override
	public void fixLegacyTypes(Recursor<ReadContext, ReadInfo> recursor, Object value) {
		if (value == null) return;
		LegacyCollectionInstance legacyInstance = (LegacyCollectionInstance) value;
		legacyInstance.newCollection = constructCollectionWithSize(Array.getLength(legacyInstance.legacyArray));
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
