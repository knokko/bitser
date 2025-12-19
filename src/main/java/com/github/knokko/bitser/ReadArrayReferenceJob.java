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
		ReferenceTracker.LabelTargets targets = deserializer.references.get(elementsWrapper);

		int size = Array.getLength(array);
		for (int index = 0; index < size; index++) {
			Array.set(array, index, targets.get(elementsWrapper, deserializer.input));
		}
	}
}
