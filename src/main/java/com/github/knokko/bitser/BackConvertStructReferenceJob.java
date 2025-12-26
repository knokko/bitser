package com.github.knokko.bitser;

import java.lang.reflect.Field;

record BackConvertStructReferenceJob(
		Object modernObject, Field modernField,
		Object legacyReference, RecursionNode node
) {

	void convert(BackDeserializer deserializer) throws Throwable {
		modernField.set(modernObject, deserializer.references.getModern(legacyReference));
	}
}
