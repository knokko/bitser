package com.github.knokko.bitser;


import java.lang.reflect.Array;

class ReadArrayReferenceJob {

	final Object array;
	final ReferenceFieldWrapper elementsWrapper;
	final RecursionNode node;

	ReadArrayReferenceJob(Object array, ReferenceFieldWrapper elementsWrapper, RecursionNode node) {
		this.array = array;
		this.elementsWrapper = elementsWrapper;
		this.node = node;
	}

	void resolve(Deserializer deserializer) throws Throwable {
		int size = Array.getLength(array);
		for (int index = 0; index < size; index++) {
			if (ReadHelper.readOptional(deserializer.input, elementsWrapper.field.optional)) continue;
			Array.set(array, index, deserializer.references.get(elementsWrapper, deserializer.input));
		}
	}
}
