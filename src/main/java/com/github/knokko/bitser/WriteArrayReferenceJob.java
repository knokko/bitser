package com.github.knokko.bitser;

import java.lang.reflect.Array;

class WriteArrayReferenceJob {

	final Object array;
	final ReferenceFieldWrapper elementsWrapper;
	final RecursionNode node;

	WriteArrayReferenceJob(Object array, ReferenceFieldWrapper elementsWrapper, RecursionNode node) {
		this.array = array;
		this.elementsWrapper = elementsWrapper;
		this.node = node;
	}

	void save(Serializer serializer) throws Throwable {
		int length = Array.getLength(array);
		ReferenceTracker.LabelTargets targets = serializer.references.get(elementsWrapper);
		for (int index = 0; index < length; index++) {
			targets.save(elementsWrapper, Array.get(array, index), serializer.output);
		}
	}
}
