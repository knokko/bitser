package com.github.knokko.bitser;

class BackReadStructReferenceJob {

	final Object[] legacyValuesArray;
	final int legacyValuesIndex;
	final ReferenceFieldWrapper legacyWrapper;
	final RecursionNode node;

	BackReadStructReferenceJob(
			Object[] legacyValuesArray, int legacyValuesIndex,
			ReferenceFieldWrapper legacyWrapper, RecursionNode node
	) {
		this.legacyValuesArray = legacyValuesArray;
		this.legacyValuesIndex = legacyValuesIndex;
		this.legacyWrapper = legacyWrapper;
		this.node = node;
	}

	void resolve(BackDeserializer deserializer) throws Throwable {
		legacyValuesArray[legacyValuesIndex] = deserializer.references.getWithOrLegacy(
				legacyWrapper, deserializer.input
		);
	}
}
