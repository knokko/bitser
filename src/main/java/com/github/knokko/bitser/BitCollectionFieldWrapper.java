package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.LegacyBitserException;
import com.github.knokko.bitser.legacy.LegacyArrayValue;
import com.github.knokko.bitser.field.ClassField;
import com.github.knokko.bitser.field.IntegerField;

import java.lang.reflect.Array;
import java.util.*;

@BitStruct(backwardCompatible = false)
class BitCollectionFieldWrapper extends AbstractCollectionFieldWrapper {

	@ClassField(root = BitFieldWrapper.class)
	private final BitFieldWrapper valuesWrapper;

	BitCollectionFieldWrapper(VirtualField field, IntegerField sizeField, BitFieldWrapper valuesWrapper) {
		super(field, sizeField);
		this.valuesWrapper = valuesWrapper;
	}

	@SuppressWarnings("unused")
	private BitCollectionFieldWrapper() {
		super();
		this.valuesWrapper = null;
	}

	@Override
	ArrayType determineArrayType() {
		return null;
	}

	@Override
	void registerLegacyClasses(UsedStructCollector collector) {
		valuesWrapper.registerLegacyClasses(collector);
	}

	@Override
	void registerReferenceTargets(
			AbstractReferenceTracker references, Object value,
			RecursionNode parentNode, String fieldName
	) {
		RecursionNode childNode = new RecursionNode(parentNode, fieldName);
		if (field.type.isArray()) {
			references.arrayJobs.add(new WithArrayJob(value, valuesWrapper, childNode));
		} else {
			references.arrayJobs.add(new WithArrayJob(((Collection<?>) value).toArray(), valuesWrapper, childNode));
		}
	}

	@Override
	public void write(Serializer serializer, Object value, RecursionNode parentNode, String fieldName) throws Throwable {
		RecursionNode childNode = new RecursionNode(parentNode, fieldName);
		if (field.type.isArray()) {
			int length = Array.getLength(value);
			if (serializer.intDistribution != null) {
				serializer.intDistribution.insert(field + " collection size", (long) length, sizeField);
				serializer.intDistribution.insert("collection size", (long) length, sizeField);
			}

			serializer.output.prepareProperty("array-length", -1);
			IntegerBitser.encodeInteger(length, sizeField, serializer.output);
			serializer.output.finishProperty();

			if (length == 0) return;
			if (valuesWrapper instanceof ReferenceFieldWrapper) {
				serializer.arrayReferenceJobs.add(new WriteArrayReferenceJob(
						value, (ReferenceFieldWrapper) valuesWrapper,
						"this array must not have null elements", childNode
				));
			} else {
				serializer.arrayJobs.add(new WriteArrayJob(
						value, valuesWrapper, childNode, "this array must not have null elements"
				));
			}
		} else {
			Collection<?> collection = (Collection<?>) value;
			if (serializer.intDistribution != null) {
				serializer.intDistribution.insert(field + " collection size", (long) collection.size(), sizeField);
				serializer.intDistribution.insert("collection size", (long) collection.size(), sizeField);
			}

			serializer.output.prepareProperty("collection-size", -1);
			IntegerBitser.encodeInteger(collection.size(), sizeField, serializer.output);
			serializer.output.finishProperty();

			if (collection.isEmpty()) return;
			if (valuesWrapper instanceof ReferenceFieldWrapper) {
				serializer.arrayReferenceJobs.add(new WriteArrayReferenceJob(
						collection.toArray(), (ReferenceFieldWrapper) valuesWrapper,
						"this collection must not have null elements", childNode
				));
			} else {
				serializer.arrayJobs.add(new WriteArrayJob(
						collection.toArray(), valuesWrapper, childNode,
						"this collection must not have null elements"
				));
			}
		}
	}

	@Override
	public Object read(Deserializer deserializer, RecursionNode parentNode, String fieldName) throws Throwable {
		RecursionNode childNode = new RecursionNode(parentNode, fieldName);
		if (field.type.isArray()) {
			deserializer.input.prepareProperty("array-length", -1);
			int length = IntegerBitser.decodeLength(sizeField, deserializer.sizeLimit, "array-length", deserializer.input);
			deserializer.input.finishProperty();

			Object array = Array.newInstance(field.type.getComponentType(), length);
			if (length == 0) return array;

			if (valuesWrapper instanceof ReferenceFieldWrapper) {
				deserializer.arrayReferenceJobs.add(new ReadArrayReferenceJob(
						array, (ReferenceFieldWrapper) valuesWrapper, childNode
				));
			} else {
				deserializer.arrayJobs.add(new ReadArrayJob(
						array, valuesWrapper, childNode
				));
			}

			return array;
		} else {
			deserializer.input.prepareProperty("collection-size", -1);
			int size = IntegerBitser.decodeLength(sizeField, deserializer.sizeLimit, "collection-size", deserializer.input);
			deserializer.input.finishProperty();

			Object[] array = new Object[size];
			Collection<?> collection = (Collection<?>) constructCollectionWithSize(field.type, size);
			if (size == 0) return collection;

			if (valuesWrapper instanceof ReferenceFieldWrapper) {
				deserializer.arrayReferenceJobs.add(new ReadArrayReferenceJob(
						array, (ReferenceFieldWrapper) valuesWrapper, childNode
				));
			} else {
				deserializer.arrayJobs.add(new ReadArrayJob(
						array, valuesWrapper, childNode
				));
			}
			deserializer.populateJobs.add(new PopulateCollectionJob(collection, array, childNode));

			return collection;
		}
	}

	@Override
	public Object read(BackDeserializer deserializer, RecursionNode parentNode, String fieldName) throws Throwable {
		RecursionNode childNode = new RecursionNode(parentNode, fieldName);
		deserializer.input.prepareProperty("legacy-array-length", -1);
		int length = IntegerBitser.decodeLength(sizeField, deserializer.sizeLimit, "legacy-array-length", deserializer.input);
		deserializer.input.finishProperty();

		Object array = constructCollectionWithSize(length);
		if (length == 0) return new LegacyArrayValue(array);

		if (valuesWrapper instanceof ReferenceFieldWrapper) {
			deserializer.arrayReferenceJobs.add(new BackReadArrayReferenceJob(
					array, (ReferenceFieldWrapper) valuesWrapper, childNode
			));
		} else {
			deserializer.arrayJobs.add(new BackReadArrayJob(
					array, valuesWrapper, childNode
			));
		}

		return new LegacyArrayValue(array);
	}

	@Override
	Object convert(BackDeserializer deserializer, Object legacyValue, RecursionNode parentNode, String fieldName) {
		if (!(legacyValue instanceof LegacyArrayValue)) {
			throw new LegacyBitserException("Can't convert from legacy " + legacyValue +
					" to collection/array for field " + field);
		}
		Object legacyArray = ((LegacyArrayValue) legacyValue).array;
		int length = Array.getLength(legacyArray);

		RecursionNode childNode = new RecursionNode(parentNode, fieldName);
		if (field.type.isArray()) {
			Object modernArray = Array.newInstance(field.type.getComponentType(), length);
			if (valuesWrapper instanceof ReferenceFieldWrapper) {
				deserializer.convertArrayReferenceJobs.add(new BackConvertArrayReferenceJob(
						legacyArray, modernArray, (ReferenceFieldWrapper) valuesWrapper, childNode
				));
			} else {
				deserializer.convertArrayJobs.add(new BackConvertArrayJob(
						legacyArray, modernArray, valuesWrapper, childNode
				));
			}

			return modernArray;
		} else {
			Object modernArray = Array.newInstance(valuesWrapper.field.type, length);
			if (valuesWrapper instanceof ReferenceFieldWrapper) {
				deserializer.convertArrayReferenceJobs.add(new BackConvertArrayReferenceJob(
						legacyArray, modernArray, (ReferenceFieldWrapper) valuesWrapper, childNode
				));
			} else {
				deserializer.convertArrayJobs.add(new BackConvertArrayJob(
						legacyArray, modernArray, valuesWrapper, childNode
				));
			}

			Object modernCollection = constructCollectionWithSize(length);
			deserializer.populateJobs.add(new PopulateCollectionJob(
					(Collection<?>) modernCollection, (Object[]) modernArray, childNode)
			);
			return modernCollection;
		}
	}

	@Override
	boolean deepEquals(Object a, Object b, BitserCache cache) {
		if (a == null && b == null) return true;
		if (a == null || b == null) return false;
		if (field.type.isArray()) {
			int length = Array.getLength(a);
			if (Array.getLength(b) != length) return false;
			for (int index = 0; index < length; index++) {
				if (!valuesWrapper.deepEquals(Array.get(a, index), Array.get(b, index), cache)) return false;
			}
		} else {
			Iterator<?> iteratorA = ((Collection<?>) a).iterator();
			Iterator<?> iteratorB = ((Collection<?>) b).iterator();
			while (true) {
				if (iteratorA.hasNext() != iteratorB.hasNext()) return false;
				if (!iteratorA.hasNext()) break;
				if (!valuesWrapper.deepEquals(iteratorA.next(), iteratorB.next(), cache)) return false;
			}
		}

		return true;
	}

	@Override
	int hashCode(Object value, BitserCache cache) {
		if (value == null) return -10;

		int code = 7;
		if (field.type.isArray()) {
			int length = Array.getLength(value);
			for (int index = 0; index < length; index++) {
				code = 29 * code + valuesWrapper.hashCode(Array.get(value, index), cache);
			}
		} else {
			for (Object element : (Collection<?>) value) {
				code = 23 * code + valuesWrapper.hashCode(element, cache);
			}
		}

		return code;
	}
}
