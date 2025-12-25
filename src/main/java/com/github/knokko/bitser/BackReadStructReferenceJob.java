package com.github.knokko.bitser;

import com.github.knokko.bitser.legacy.BackReference;

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
		Object legacyReference = deserializer.references.get(legacyWrapper).getLegacy(legacyWrapper, deserializer.input);
		legacyValuesArray[legacyValuesIndex] = new BackReference(legacyReference);
	}
}
