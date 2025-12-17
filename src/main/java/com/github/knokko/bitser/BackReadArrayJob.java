package com.github.knokko.bitser;

import java.lang.reflect.Array;

class BackReadArrayJob {

	final Object array;
	final BitFieldWrapper elementsWrapper;
	final RecursionNode node;

	BackReadArrayJob(Object array, BitFieldWrapper elementsWrapper, RecursionNode node) {
		this.array = array;
		this.elementsWrapper = elementsWrapper;
		this.node = node;
	}

	void read(BackDeserializer deserializer) throws Throwable {
		int size = Array.getLength(array);
		for (int index = 0; index < size; index++) {
			if (elementsWrapper.field.optional && !deserializer.input.read()) continue;

			deserializer.input.pushContext(node, "element");
			Object element = elementsWrapper.read(deserializer, node, "elements");
			deserializer.input.popContext(node, "element");

			Array.set(array, index, element);
			if (elementsWrapper.field.referenceTargetLabel != null) {
				deserializer.references.registerLegacyTarget(
						elementsWrapper.field.referenceTargetLabel, element
				);
			}
		}
	}
}
