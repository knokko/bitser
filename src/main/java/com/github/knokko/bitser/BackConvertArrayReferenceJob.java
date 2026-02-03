package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.LegacyBitserException;
import com.github.knokko.bitser.legacy.LegacyReference;
import com.github.knokko.bitser.legacy.WithReference;

import java.lang.reflect.Array;

record BackConvertArrayReferenceJob(
		Object legacyArray, Object modernArray,
		ReferenceFieldWrapper modernWrapper, RecursionNode node
) {

	void convert(BackDeserializer deserializer) {
		int length = Array.getLength(legacyArray);
		for (int index = 0; index < length; index++) {
			Object legacyElement = Array.get(legacyArray, index);
			if (legacyElement == null) continue;

			if (legacyElement instanceof LegacyReference) {
				Object modernElement = deserializer.references.getModern(((LegacyReference) legacyElement).reference());
				Array.set(modernArray, index, modernElement);
			} else if (legacyElement instanceof WithReference) {
				Array.set(modernArray, index, ((WithReference) legacyElement).reference());
			} else {
				throw new LegacyBitserException("Can't convert from legacy " + legacyElement + " to reference");
			}
		}
	}
}
