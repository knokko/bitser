package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.exceptions.LegacyBitserException;
import com.github.knokko.bitser.exceptions.UnexpectedBitserException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.util.JobOutput;
import com.github.knokko.bitser.util.Recursor;

import java.util.Objects;
import java.util.function.Consumer;

abstract class BitFieldWrapper {

	@SuppressWarnings("unused")
	private static final Class<?>[] BITSER_HIERARCHY = {
			BooleanFieldWrapper.class, IntegerFieldWrapper.class, FloatFieldWrapper.class,
			StringFieldWrapper.class, UUIDFieldWrapper.class, EnumFieldWrapper.class,
			StructFieldWrapper.class,
			LazyFieldWrapper.class,
			BitCollectionFieldWrapper.class, ByteCollectionFieldWrapper.class, MapFieldWrapper.class,
			StableReferenceFieldWrapper.class, UnstableReferenceFieldWrapper.class
	};

	@BitField
	final VirtualField field;

	BitFieldWrapper(VirtualField field) {
		this.field = field;
		if (field.optional && field.type.isPrimitive()) {
			throw new InvalidBitFieldException("Primitive field " + field + " can't be optional");
		}
	}

	BitFieldWrapper() {
		this.field = new VirtualField();
	}

	BitFieldWrapper getChildWrapper() {
		throw new UnexpectedBitserException("getChildWrapper only works on collection types, but this is " + getClass());
	}

	void collectReferenceLabels(Recursor<LabelContext, LabelInfo> recursor) {
		if (field.referenceTargetLabel != null) {
			recursor.runFlat("referenceTargetLabel", context ->
					context.declaredTargets.add(field.referenceTargetLabel)
			);
		}
	}

	void registerReferenceTargets(Object value, Recursor<ReferenceIdMapper, BitserCache> recursor) {
		if (field.referenceTargetLabel != null && value != null) {
			recursor.runFlat("referenceTargetLabel", mapper ->
					mapper.register(field.referenceTargetLabel, value, recursor.info)
			);
		}
	}

	void registerLegacyClasses(Object value, Recursor<LegacyClasses, LegacyInfo> recursor) {}

	void writeField(Object object, Recursor<WriteContext, WriteInfo> recursor) {
		Object value = field.getValue.apply(object);
		if (field.optional) {
			recursor.runFlat("not-null", context -> {
				context.output.prepareProperty("not-null", -1);
				context.output.write(value != null);
				context.output.finishProperty();
			});
		}
		if (value == null) {
			if (!field.optional) {
				throw new InvalidBitValueException("Field " + field + " of " + object + " must not be null");
			}
		} else {
			writeValue(value, recursor);
			if (field.referenceTargetLabel != null) {
				recursor.runFlat("referenceTargetLabel", context ->
						context.idMapper.maybeEncodeUnstableId(
								field.referenceTargetLabel, value, context.output, -1
						)
				);
			}
		}
	}

	abstract void writeValue(Object value, Recursor<WriteContext, WriteInfo> recursor) ;

	final void readField(Recursor<ReadContext, ReadInfo> recursor, Consumer<Object> setValue) {
		JobOutput<Boolean> hasValue = recursor.computeFlat("optional", context -> {
			if (field.optional) {
				return context.input.read();
			} else return true;
		});
		recursor.runNested("value", nested -> {
			if (!hasValue.get()) return;
			readValue(nested, value -> {
				setValue.accept(value);
				if (field.referenceTargetLabel != null) {
					recursor.runFlat("referenceTargetLabel", context -> {
						try {
							context.idLoader.register(field.referenceTargetLabel, value, context.input, nested.info.bitser.cache);
						} catch (InvalidBitValueException missingID) {
							throw new InvalidBitFieldException("Missing stable ID for legacy field with label " + field.referenceTargetLabel);
						}
					});
				}
			});
		});
	}

	final void readField(Object object, Recursor<ReadContext, ReadInfo> recursor) {
		readField(recursor, value -> field.setValue.accept(object, value));
	}

	abstract void readValue(Recursor<ReadContext, ReadInfo> recursor, Consumer<Object> setValue);

	void registerReferenceTargets(
			AbstractReferenceTracker references, Object value,
			RecursionNode parentNode, String fieldName
	) {}

	abstract void write(
			Serializer serializer, Object value,
			RecursionNode parentNode, String fieldName
	) throws Throwable;

	abstract Object read(Deserializer deserializer, RecursionNode parentNode, String fieldName) throws Throwable;

	abstract Object read(BackDeserializer deserializer, RecursionNode parentNode, String fieldName) throws Throwable;

	abstract Object convert(BackDeserializer deserializer, Object legacyValue, RecursionNode parentNode, String fieldName);

	void setLegacyValue(Recursor<ReadContext, ReadInfo> recursor, Object value, Consumer<Object> setValue) {
		if (!field.optional && value == null) {
			throw new LegacyBitserException("Legacy value for field " + field + " is null, which is no longer allowed");
		}
		try {
			setValue.accept(value);
		} catch (IllegalArgumentException invalidType) {
			throw new LegacyBitserException("Can't convert from legacy " + value + " to " + field.type + " for field " + field);
		}
	}

	void fixLegacyTypes(Recursor<ReadContext, ReadInfo> recursor, Object value) {}

	boolean isReference() {
		return false;
	}

	boolean deepEquals(Object a, Object b, BitserCache cache) {
		return Objects.equals(a, b);
	}

	int hashCode(Object value, BitserCache cache) {
		return Objects.hashCode(value);
	}
}
