package com.github.knokko.bitser;

import com.github.knokko.bitser.legacy.BackReference;

import java.lang.reflect.Array;

class BackConvertArrayReferenceJob {

	final Object legacyArray;
	final Object modernArray;
	final ReferenceFieldWrapper modernWrapper;
	final RecursionNode node;

	BackConvertArrayReferenceJob(
			Object legacyArray, Object modernArray,
			ReferenceFieldWrapper modernWrapper, RecursionNode node
	) {
		this.legacyArray = legacyArray;
		this.modernArray = modernArray;
		this.modernWrapper = modernWrapper;
		this.node = node;
	}

	void convert(BackDeserializer deserializer) {
		int length = Array.getLength(legacyArray);
		for (int index = 0; index < length; index++) {
			Object legacyElement = Array.get(legacyArray, index);
			if (legacyElement instanceof BackReference) {
				Object modernElement = deserializer.references.getModern(((BackReference) legacyElement).reference);
				Array.set(modernArray, index, modernElement);
			} else {
				Array.set(modernArray, index, ((WithReference) legacyElement).reference);
			}
		}
	}
}
