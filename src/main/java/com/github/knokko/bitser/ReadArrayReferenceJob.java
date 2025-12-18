package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.ReferenceBitserException;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

class ReadArrayReferenceJob {

	final Object array;
	final ReferenceFieldWrapper elementsWrapper;
	final RecursionNode node;

	ReadArrayReferenceJob(Object array, ReferenceFieldWrapper elementsWrapper, RecursionNode node) {
		this.array = array;
		this.elementsWrapper = elementsWrapper;
		this.node = node;
	}

	void resolve(Deserializer deserializer) throws Throwable {
		HashMap<UUID, Object> stableMap = null;
		ArrayList<Object> unstableList = null;
		if (elementsWrapper instanceof StableReferenceFieldWrapper) {
			stableMap = deserializer.references.stableTargets.get(elementsWrapper.label);
			if (stableMap == null) {
				throw new ReferenceBitserException("ehm");
			}
		} else {
			unstableList = deserializer.references.unstableTargets.get(elementsWrapper.label);
			if (unstableList == null) {
				throw new ReferenceBitserException("ehm");
			}
		}

		int size = Array.getLength(array);
		for (int index = 0; index < size; index++) {
			if (elementsWrapper.field.optional && !deserializer.input.read()) continue;

			Object value;
			if (elementsWrapper instanceof StableReferenceFieldWrapper) {
				UUID id = new UUID(
						IntegerBitser.decodeFullLong(deserializer.input),
						IntegerBitser.decodeFullLong(deserializer.input)
				);
				value = stableMap.get(id);
				if (value == null) {
					throw new ReferenceBitserException("ehm");
				}

			} else {
				int referenceIndex = (int) IntegerBitser.decodeUniformInteger(
						0, unstableList.size() - 1, deserializer.input
				);
				value = unstableList.get(referenceIndex);
			}

			Array.set(array, index, value);
		}
	}
}
