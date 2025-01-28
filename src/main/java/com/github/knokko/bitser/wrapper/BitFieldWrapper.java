package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.backward.LegacyClasses;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.serialize.BitserCache;
import com.github.knokko.bitser.serialize.LabelCollection;
import com.github.knokko.bitser.serialize.ReadJob;
import com.github.knokko.bitser.serialize.WriteJob;
import com.github.knokko.bitser.util.ReferenceIdMapper;
import com.github.knokko.bitser.util.VirtualField;

import java.io.IOException;
import java.util.function.Consumer;

public abstract class BitFieldWrapper {

	@SuppressWarnings("unused")
	private static final Class<?>[] BITSER_HIERARCHY = {
			BooleanFieldWrapper.class, IntegerFieldWrapper.class, FloatFieldWrapper.class,
			StringFieldWrapper.class, UUIDFieldWrapper.class, EnumFieldWrapper.class,
			StructFieldWrapper.class,
			BitCollectionFieldWrapper.class, ByteCollectionFieldWrapper.class, MapFieldWrapper.class,
			StableReferenceFieldWrapper.class, UnstableReferenceFieldWrapper.class
	};

	@BitField
	public final VirtualField field;

	BitFieldWrapper(VirtualField field) {
		this.field = field;
		if (field.optional && field.type.isPrimitive()) {
			throw new InvalidBitFieldException("Primitive field " + field + " can't be optional");
		}
	}

	BitFieldWrapper() {
		this.field = new VirtualField();
	}

	public BitFieldWrapper getChildWrapper() {
		throw new UnsupportedOperationException("getChildWrapper only works on collection types, but this is " + getClass());
	}

	public void collectReferenceLabels(LabelCollection labels) {
		if (field.referenceTargetLabel != null) labels.declaredTargets.add(field.referenceTargetLabel);
	}

	public void collectUsedReferenceLabels(LabelCollection labels, Object value) {
		if (value != null && field.referenceTargetLabel != null) {
			labels.declaredTargets.add(field.referenceTargetLabel);
		}
	}

	void registerReferenceTargets(Object value, BitserCache cache, ReferenceIdMapper idMapper) {
		if (field.referenceTargetLabel != null && value != null) {
			idMapper.register(field.referenceTargetLabel, value, cache);
		}
	}

	void registerLegacyClasses(Object value, LegacyClasses legacy) {}

	public final void write(Object object, WriteJob write) throws IOException {
		try {
			writeField(object, write);
		} catch (InvalidBitValueException invalidValue) {
			throw new InvalidBitValueException(invalidValue.getMessage() + " for " + field);
		}
	}

	void writeField(Object object, WriteJob write) throws IOException {
		Object value = field.getValue.apply(object);
		if (field.optional) write.output.write(value != null);
		if (value == null) {
			if (!field.optional) {
				throw new InvalidBitValueException("Field " + field + " of " + object + " must not be null");
			}
		} else {
			writeValue(value, write);
			if (field.referenceTargetLabel != null) {
				write.idMapper.maybeEncodeUnstableId(field.referenceTargetLabel, value, write.output);
			}
		}
	}

	abstract void writeValue(Object value, WriteJob write) throws IOException;

	public final void read(ReadJob read, ValueConsumer setValue) throws IOException {
		readField(read, setValue);
	}

	final void readField(ReadJob read, ValueConsumer setValue) throws IOException {
		if (field.optional && !read.input.read()) setValue.consume(null);
		else {
			readValue(read, value -> {
				setValue.consume(value);
				if (field.referenceTargetLabel != null) {
					try {
						read.idLoader.register(field.referenceTargetLabel, value, read.input, read.cache);
					} catch (InvalidBitValueException missingID) {
						throw new InvalidBitFieldException("Missing stable ID for legacy field with label " + field.referenceTargetLabel);
					}
				}
			});
		}
	}

	final void readField(Object object, ReadJob read) throws IOException {
		readField(read, value -> field.setValue.accept(object, value));
	}

	abstract void readValue(ReadJob read, ValueConsumer setValue) throws IOException;

	Object readLegacyValue(ReadJob read) throws IOException {
		Object[] pResult = { null, null };
		readValue(read, value -> {
			pResult[0] = value;
			pResult[1] = this;
		});
		if (pResult[1] == null) throw new Error("readValue of " + getClass().getSimpleName() + " must be instant");
		return pResult[0];
	}

	void setLegacyValue(ReadJob read, Object value, Consumer<Object> setValue) {
		if (!field.optional && value == null) {
			throw new InvalidBitValueException("Legacy value for field " + field + " is null, which is no longer allowed");
		}
		try {
			setValue.accept(value);
		} catch (IllegalArgumentException invalidType) {
			throw new InvalidBitFieldException("Can't convert from legacy " + value + " to " + field.type + " for field " + field);
		}
	}

	public void fixLegacyTypes(ReadJob read, Object value) {}

	public boolean isReference() {
		return false;
	}
}
