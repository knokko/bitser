package com.github.knokko.bitser;

import java.lang.reflect.Array;

class ReadArrayJob {

	final Object array;
	final BitFieldWrapper elementsWrapper;
	final RecursionNode node;

	ReadArrayJob(Object array, BitFieldWrapper elementsWrapper, RecursionNode node) {
		this.array = array;
		this.elementsWrapper = elementsWrapper;
		this.node = node;
	}

	void read(Deserializer deserializer) throws Throwable {
		int size = Array.getLength(array);
		for (int index = 0; index < size; index++) {
			if (elementsWrapper.field.optional && !deserializer.input.read()) continue;

			Object element = elementsWrapper.read(deserializer, node, "elements");
			Array.set(array, index, element);
			if (elementsWrapper.field.referenceTargetLabel != null) {
				deserializer.references.registerTarget(elementsWrapper.field.referenceTargetLabel, element);
			}
		}
	}
}
