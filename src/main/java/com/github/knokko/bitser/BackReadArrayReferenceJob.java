package com.github.knokko.bitser;

import java.lang.reflect.Array;

class BackReadArrayReferenceJob {

	final Object array;
	final ReferenceFieldWrapper elementsWrapper;
	final RecursionNode node;

	BackReadArrayReferenceJob(Object array, ReferenceFieldWrapper elementsWrapper, RecursionNode node) {
		this.array = array;
		this.elementsWrapper = elementsWrapper;
		this.node = node;
	}

	void resolve(BackDeserializer deserializer) throws Throwable {
		int size = Array.getLength(array);
		for (int index = 0; index < size; index++) {
			if (ReadHelper.readOptional(deserializer.input, elementsWrapper.field.optional)) continue;
			Array.set(array, index, deserializer.references.getWithOrLegacy(elementsWrapper, deserializer.input));
		}
	}
}
