package com.github.knokko.bitser;

import java.lang.reflect.Field;

class BackConvertStructReferenceJob {

	final Object modernObject;
	final Field modernField;
	final Object legacyReference;
	final RecursionNode node;

	BackConvertStructReferenceJob(Object modernObject, Field modernField, Object legacyReference, RecursionNode node) {
		this.modernObject = modernObject;
		this.modernField = modernField;
		this.legacyReference = legacyReference;
		this.node = node;
	}

	void convert(BackDeserializer deserializer) throws Throwable {
		modernField.set(modernObject, deserializer.references.getModern(legacyReference));
	}
}
