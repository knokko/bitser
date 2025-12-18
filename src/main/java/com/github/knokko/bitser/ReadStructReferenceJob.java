package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.ReferenceBitserException;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

class ReadStructReferenceJob {

	final Object structObject;
	final String label;
	final boolean stable;
	final Field classField;
	final RecursionNode node;

	ReadStructReferenceJob(Object structObject, String label, boolean stable, Field classField, RecursionNode node) {
		this.structObject = structObject;
		this.label = label;
		this.stable = stable;
		this.classField = classField;
		this.node = node;
	}

	void resolve(Deserializer deserializer) throws Throwable {
		if (stable) {
			UUID id = new UUID(
					IntegerBitser.decodeFullLong(deserializer.input),
					IntegerBitser.decodeFullLong(deserializer.input)
			);
			HashMap<UUID, Object> stableMap = deserializer.references.stableTargets.get(label);
			if (stableMap == null) {
				throw new ReferenceBitserException("ehm");
			}
			Object value = stableMap.get(id);
			if (value == null) {
				throw new ReferenceBitserException("ehm");
			}
			classField.set(structObject, value);
		} else {
			ArrayList<Object> references = deserializer.references.unstableTargets.get(label);
			if (references == null) {
				throw new ReferenceBitserException("ehm");
			}
			int index = (int) IntegerBitser.decodeUniformInteger(
					0, references.size() - 1, deserializer.input
			);
			classField.set(structObject, references.get(index));
		}
	}
}
