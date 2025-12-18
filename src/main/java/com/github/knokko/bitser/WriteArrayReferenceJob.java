package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.exceptions.ReferenceBitserException;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

class WriteArrayReferenceJob {

	final Object array;
	final ReferenceFieldWrapper elementsWrapper;
	final RecursionNode node;

	WriteArrayReferenceJob(Object array, ReferenceFieldWrapper elementsWrapper, RecursionNode node) {
		this.array = array;
		this.elementsWrapper = elementsWrapper;
		this.node = node;
	}

	void save(Serializer serializer) throws Throwable {
		int length = Array.getLength(array);
		if (elementsWrapper instanceof StableReferenceFieldWrapper) {
			BitStructWrapper<?> valueInfo = serializer.cache.getWrapperOrNull(elementsWrapper.field.type);
			if (valueInfo == null) {
				throw new InvalidBitFieldException("Can't find stable ID of " + elementsWrapper.field.type + " because it's not a BitStruct");
			}
			if (!valueInfo.hasStableId()) {
				throw new InvalidBitFieldException("Can't find StableReferenceFieldId of " + valueInfo);
			}
			HashMap<UUID, Object> stableMap = serializer.references.stableTargets.get(elementsWrapper.label);
			if (stableMap == null) {
				throw new ReferenceBitserException("ehm");
			}

			for (int index = 0; index < length; index++) {
				Object element = Array.get(array, index);
				if (elementsWrapper.field.optional) {
					serializer.output.write(element != null);
					if (element == null) continue;
				} else if (element == null) throw new InvalidBitValueException("ehm");
				UUID id = valueInfo.getStableId(element);
				IntegerBitser.encodeFullLong(id.getMostSignificantBits(), serializer.output);
				IntegerBitser.encodeFullLong(id.getLeastSignificantBits(), serializer.output);

				Object actualValue = stableMap.get(id);
				if (actualValue != element) {
					throw new ReferenceBitserException("ehm");
				}
			}
		} else {
			ArrayList<Object> references = serializer.references.unstableTargets.get(elementsWrapper.label);
			if (references == null) {
				throw new ReferenceBitserException("ehm");
			}

			for (int index = 0; index < length; index++) {
				Object element = Array.get(array, index);
				if (elementsWrapper.field.optional) {
					serializer.output.write(element != null);
					if (element == null) continue;
				} else if (element == null) throw new InvalidBitValueException("ehm");

				// TODO Use a hash-code wrapper for this, and optimize
				// TODO Also validate that value was registered as target
				int referenceIndex = references.indexOf(element);
				IntegerBitser.encodeUniformInteger(referenceIndex, 0, references.size() - 1, serializer.output);
			}
		}
	}
}
