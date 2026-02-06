package com.github.knokko.bitser;

import java.lang.reflect.Field;

record BackConvertStructReferenceJob(
		Object modernObject, Field modernField,
		Object[] modernValues, int id,
		Object legacyReference, RecursionNode node
) {

	void convert(BackDeserializer deserializer) throws Throwable {
		Object modernReference = deserializer.references.getModern(legacyReference);
		modernValues[id] = modernReference;
		if (modernField != null) modernField.set(modernObject, modernReference);
	}
}
