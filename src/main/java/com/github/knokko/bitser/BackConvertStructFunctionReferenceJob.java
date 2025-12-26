package com.github.knokko.bitser;

record BackConvertStructFunctionReferenceJob(
		Object[] modernFunctionValues, int functionID,
		Object legacyReference, RecursionNode node
) {

	void convert(BackDeserializer deserializer) throws Throwable {
		modernFunctionValues[functionID] = deserializer.references.getModern(legacyReference);
	}
}
