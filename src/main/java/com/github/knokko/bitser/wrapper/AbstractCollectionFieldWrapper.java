package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.BitserCache;
import com.github.knokko.bitser.util.ReferenceIdLoader;
import com.github.knokko.bitser.util.ReferenceIdMapper;
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
			Object element, BitFieldWrapper wrapper, BitOutputStream output,
			BitserCache cache, ReferenceIdMapper idMapper, String nullErrorMessage
	) throws IOException {
		if (wrapper.field.optional) output.write(element != null);
		else if (element == null) throw new InvalidBitValueException(nullErrorMessage);
		if (element != null) {
			wrapper.writeValue(element, output, cache, idMapper);
			if (wrapper.field.referenceTargetLabel != null) {
				idMapper.maybeEncodeUnstableId(wrapper.field.referenceTargetLabel, element, output);
			}
		}
	}

	static Object constructCollectionWithSize(VirtualField field, int size) {
		try {
			return field.type.getConstructor(int.class).newInstance(size);
		} catch (NoSuchMethodException noIntConstructor) {
			try {
				return field.type.getConstructor().newInstance();
			} catch (Exception unexpected) {
				throw new RuntimeException(unexpected);
			}
		} catch (Exception unexpected) {
			throw new RuntimeException(unexpected);
		}
	}

	private final IntegerField sizeField;

	AbstractCollectionFieldWrapper(VirtualField field, IntegerField sizeField) {
		super(field);
		if (sizeField.minValue() > Integer.MAX_VALUE || sizeField.maxValue() < 0) throw new IllegalArgumentException();
		if (!field.type.isArray() && (field.type.isInterface() || Modifier.isAbstract(field.type.getModifiers()))) {
			throw new InvalidBitFieldException("Field type must not be abstract or an interface: " + field);
		}
		this.sizeField = sizeField;
	}

	@Override
	void writeValue(
			Object value, BitOutputStream output, BitserCache cache, ReferenceIdMapper idMapper
	) throws IOException {
		int size = getCollectionSize(value);
		if (sizeField.expectUniform()) encodeUniformInteger(size, getMinSize(), getMaxSize(), output);
		else encodeVariableInteger(size, getMinSize(), getMaxSize(), output);

		writeValue(value, size, output, cache, idMapper);
	}

	@Override
	void readValue(
			BitInputStream input, BitserCache cache, ReferenceIdLoader idLoader, ValueConsumer setValue
	) throws IOException {
		int size;
		if (sizeField.expectUniform()) size = (int) decodeUniformInteger(getMinSize(), getMaxSize(), input);
		else size = (int) decodeVariableInteger(getMinSize(), getMaxSize(), input);

		Object value = constructCollectionWithSize(size);
		readValue(value, size, input, cache, idLoader);
		setValue.consume(value);
	}

	abstract void writeValue(
			Object value, int size, BitOutputStream output, BitserCache cache, ReferenceIdMapper idMapper
	) throws IOException;

	abstract void readValue(
			Object value, int size, BitInputStream input, BitserCache cache, ReferenceIdLoader idLoader
	) throws IOException;

	private int getCollectionSize(Object object) {
		if (object instanceof Collection<?>) return ((Collection<?>) object).size();
		return Array.getLength(object);
	}

	private Object constructCollectionWithSize(int size) {
		if (field.type.isArray()) {
			return Array.newInstance(field.type.getComponentType(), size);
		} else {
			return constructCollectionWithSize(field, size);
		}
	}

	private int getMinSize() {
		return (int) max(0, sizeField.minValue());
	}

	private int getMaxSize() {
		return (int) min(Integer.MAX_VALUE, sizeField.maxValue());
	}
}
