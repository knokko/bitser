package com.github.knokko.bitser;

class BackConvertStructFunctionReferenceJob {

	final Object[] modernFunctionValues;
	final int functionID;
	final Object legacyReference;
	final RecursionNode node;

	BackConvertStructFunctionReferenceJob(
			Object[] modernFunctionValues, int functionID,
			Object legacyReference, RecursionNode node
	) {
		this.modernFunctionValues = modernFunctionValues;
		this.functionID = functionID;
		this.legacyReference = legacyReference;
		this.node = node;
	}

	void convert(BackDeserializer deserializer) throws Throwable {
		modernFunctionValues[functionID] = deserializer.references.getModern(legacyReference);
	}
}
