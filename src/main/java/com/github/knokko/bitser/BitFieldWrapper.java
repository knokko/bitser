package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.field.BitField;

import java.util.Objects;

abstract class BitFieldWrapper {

	@SuppressWarnings("unused")
	private static final Class<?>[] BITSER_HIERARCHY = {
			BooleanFieldWrapper.class, IntegerFieldWrapper.class, FloatFieldWrapper.class,
			StringFieldWrapper.class, UUIDFieldWrapper.class, EnumFieldWrapper.class,
			BitCollectionFieldWrapper.class, ByteCollectionFieldWrapper.class, MapFieldWrapper.class,
			StructFieldWrapper.class, LazyFieldWrapper.class, ReferenceFieldWrapper.class,
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

	void registerLegacyClasses(UsedStructCollector collector) {}

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

	boolean deepEquals(Object a, Object b, BitserCache cache) {
		return Objects.equals(a, b);
	}

	int hashCode(Object value, BitserCache cache) {
		return Objects.hashCode(value);
	}
}
