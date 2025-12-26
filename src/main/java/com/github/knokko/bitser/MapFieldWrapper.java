package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.LegacyBitserException;
import com.github.knokko.bitser.legacy.BackMapValue;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.ClassField;
import com.github.knokko.bitser.field.IntegerField;

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.*;

import static com.github.knokko.bitser.AbstractCollectionFieldWrapper.constructCollectionWithSize;
import static java.lang.Math.max;
import static java.lang.Math.min;

@BitStruct(backwardCompatible = false)
class MapFieldWrapper extends BitFieldWrapper {

	@BitField
	private final IntegerField.Properties sizeField;

	@ClassField(root = BitFieldWrapper.class)
	private final BitFieldWrapper keysWrapper;

	@ClassField(root = BitFieldWrapper.class)
	private final BitFieldWrapper valuesWrapper;

	MapFieldWrapper(VirtualField field, IntegerField sizeField, BitFieldWrapper keysWrapper, BitFieldWrapper valuesWrapper) {
		super(field);
		if (sizeField.minValue() > Integer.MAX_VALUE || sizeField.maxValue() < 0) {
			throw new InvalidBitFieldException("Invalid sizeField bounds");
		}
		if (field.type.isInterface() || Modifier.isAbstract(field.type.getModifiers())) {
			throw new InvalidBitFieldException("Field type must not be abstract or an interface: " + field);
		}
		this.sizeField = new IntegerField.Properties(
				max(0, sizeField.minValue()), min(Integer.MAX_VALUE, sizeField.maxValue()),
				sizeField.expectUniform(), sizeField.digitSize(), sizeField.commonValues()
		);
		this.keysWrapper = keysWrapper;
		this.valuesWrapper = valuesWrapper;
	}

	@SuppressWarnings("unused")
	private MapFieldWrapper() {
		super();
		this.sizeField = new IntegerField.Properties();
		this.keysWrapper = null;
		this.valuesWrapper = null;
	}

	@Override
	void registerLegacyClasses(UsedStructCollector collector) {
		keysWrapper.registerLegacyClasses(collector);
		valuesWrapper.registerLegacyClasses(collector);
	}

	@Override
	void registerReferenceTargets(
			AbstractReferenceTracker references, Object value,
			RecursionNode parentNode, String fieldName
	) {
		RecursionNode childNode = new RecursionNode(parentNode, fieldName);
		Map<?, ?> map = (Map<?, ?>) value;
		references.arrayJobs.add(new WithArrayJob(map.keySet().toArray(), keysWrapper, childNode));
		references.arrayJobs.add(new WithArrayJob(map.values().toArray(), valuesWrapper, childNode));
	}

	@Override
	public void write(Serializer serializer, Object value, RecursionNode parentNode, String fieldName) throws Throwable {
		RecursionNode childNode = new RecursionNode(parentNode, fieldName);
		Map<?, ?> map = (Map<?, ?>) value;

		if (serializer.intDistribution != null) {
			serializer.intDistribution.insert(field + " map size", (long) map.size(), sizeField);
			serializer.intDistribution.insert("map size", (long) map.size(), sizeField);
		}

		serializer.output.prepareProperty("map-size", -1);
		IntegerBitser.encodeInteger(map.size(), sizeField, serializer.output);
		serializer.output.finishProperty();

		if (map.isEmpty()) return;
		if (keysWrapper instanceof ReferenceFieldWrapper) {
			serializer.arrayReferenceJobs.add(new WriteArrayReferenceJob(
					map.keySet().toArray(), (ReferenceFieldWrapper) keysWrapper,
					"this map must not contain null keys", childNode
			));
		} else {
			serializer.arrayJobs.add(new WriteArrayJob(
					map.keySet().toArray(), keysWrapper, childNode, "this map must not have null keys"
			));
		}

		if (valuesWrapper instanceof ReferenceFieldWrapper) {
			serializer.arrayReferenceJobs.add(new WriteArrayReferenceJob(
					map.values().toArray(), (ReferenceFieldWrapper) valuesWrapper,
					"this map must not contain null values", childNode
			));
		} else {
			serializer.arrayJobs.add(new WriteArrayJob(
					map.values().toArray(), valuesWrapper, childNode, "this map must not have null values"
			));
		}
	}

	@Override
	public Object read(Deserializer deserializer, RecursionNode parentNode, String fieldName) throws Throwable {
		RecursionNode childNode = new RecursionNode(parentNode, fieldName);
		deserializer.input.prepareProperty("map-size", -1);
		int size = IntegerBitser.decodeLength(sizeField, deserializer.sizeLimit, "map-size", deserializer.input);
		deserializer.input.finishProperty();

		Object[] keys = new Object[size];
		Object[] values = new Object[size];
		Map<?, ?> map = (Map<?, ?>) constructCollectionWithSize(field.type, size);
		if (size == 0) return map;

		if (keysWrapper instanceof ReferenceFieldWrapper) {
			deserializer.arrayReferenceJobs.add(new ReadArrayReferenceJob(
					keys, (ReferenceFieldWrapper) keysWrapper, childNode
			));
		} else {
			deserializer.arrayJobs.add(new ReadArrayJob(
					keys, keysWrapper, childNode
			));
		}

		if (valuesWrapper instanceof ReferenceFieldWrapper) {
			deserializer.arrayReferenceJobs.add(new ReadArrayReferenceJob(
					values, (ReferenceFieldWrapper) valuesWrapper, childNode
			));
		} else {
			deserializer.arrayJobs.add(new ReadArrayJob(
					values, valuesWrapper, childNode
			));
		}
		deserializer.populateJobs.add(new PopulateMapJob(map, keys, values, childNode));

		return map;
	}

	@Override
	public Object read(BackDeserializer deserializer, RecursionNode parentNode, String fieldName) throws Throwable {
		RecursionNode childNode = new RecursionNode(parentNode, fieldName);
		deserializer.input.prepareProperty("map-size", -1);
		int size = IntegerBitser.decodeLength(sizeField, deserializer.sizeLimit, "map-size", deserializer.input);
		deserializer.input.finishProperty();

		Object[] keys = new Object[size];
		Object[] values = new Object[size];
		if (size == 0) return new BackMapValue(keys, values);

		if (keysWrapper instanceof ReferenceFieldWrapper) {
			deserializer.arrayReferenceJobs.add(new BackReadArrayReferenceJob(
					keys, (ReferenceFieldWrapper) keysWrapper, childNode
			));
		} else {
			deserializer.arrayJobs.add(new BackReadArrayJob(
					keys, keysWrapper, childNode
			));
		}

		if (valuesWrapper instanceof ReferenceFieldWrapper) {
			deserializer.arrayReferenceJobs.add(new BackReadArrayReferenceJob(
					values, (ReferenceFieldWrapper) valuesWrapper, childNode
			));
		} else {
			deserializer.arrayJobs.add(new BackReadArrayJob(
					values, valuesWrapper, childNode
			));
		}

		return new BackMapValue(keys, values);
	}

	@Override
	Object convert(BackDeserializer deserializer, Object legacyValue, RecursionNode parentNode, String fieldName) {
		if (!(legacyValue instanceof BackMapValue)) {
			throw new LegacyBitserException("Can't convert from legacy " + legacyValue +
					" to map for field " + field);
		}
		BackMapValue legacyMap = (BackMapValue) legacyValue;
		int size = legacyMap.keys.length;

		Map<?, ?> modernMap = (Map<?, ?>) constructCollectionWithSize(field.type, size);
		Object modernKeys = Array.newInstance(keysWrapper.field.type, size);
		Object modernValues = Array.newInstance(valuesWrapper.field.type, size);

		RecursionNode childNode = new RecursionNode(parentNode, fieldName);
		if (keysWrapper instanceof ReferenceFieldWrapper) {
			deserializer.convertArrayReferenceJobs.add(new BackConvertArrayReferenceJob(
					legacyMap.keys, modernKeys, (ReferenceFieldWrapper) keysWrapper, childNode
			));
		} else {
			deserializer.convertArrayJobs.add(new BackConvertArrayJob(
					legacyMap.keys, modernKeys, keysWrapper, childNode
			));
		}

		if (valuesWrapper instanceof ReferenceFieldWrapper) {
			deserializer.convertArrayReferenceJobs.add(new BackConvertArrayReferenceJob(
					legacyMap.values, modernValues, (ReferenceFieldWrapper) valuesWrapper, childNode
			));
		} else {
			deserializer.convertArrayJobs.add(new BackConvertArrayJob(
					legacyMap.values, modernValues, valuesWrapper, childNode
			));
		}

		deserializer.populateJobs.add(new PopulateMapJob(
				modernMap, (Object[]) modernKeys, (Object[]) modernValues, childNode)
		);
		return modernMap;
	}

	@Override
	boolean deepEquals(Object a, Object b, BitserCache cache) {
		if (a == null && b == null) return true;
		if (a == null || b == null) return false;
		Iterator<? extends Map.Entry<?, ?>> iteratorA = ((Map<?, ?>) a).entrySet().iterator();
		Iterator<? extends Map.Entry<?, ?>> iteratorB = ((Map<?, ?>) b).entrySet().iterator();
		while (true) {
			if (iteratorA.hasNext() != iteratorB.hasNext()) return false;
			if (!iteratorA.hasNext()) return true;
			Map.Entry<?, ?> entryA = iteratorA.next();
			Map.Entry<?, ?> entryB = iteratorB.next();
			if (!keysWrapper.deepEquals(entryA.getKey(), entryB.getKey(), cache)) return false;
			if (!valuesWrapper.deepEquals(entryA.getValue(), entryB.getValue(), cache)) return false;
		}
	}

	@Override
	int hashCode(Object value, BitserCache cache) {
		if (value == null) return -12;

		int code = 15;
		for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
			code = 3 * code + keysWrapper.hashCode(entry.getKey(), cache);
			code = 5 * code + valuesWrapper.hashCode(entry.getValue(), cache);
		}

		return code;
	}
}
