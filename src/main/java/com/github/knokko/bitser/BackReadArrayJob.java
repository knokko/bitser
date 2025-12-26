package com.github.knokko.bitser;

import java.lang.reflect.Array;

record BackReadArrayJob(Object array, BitFieldWrapper elementsWrapper, RecursionNode node) {

	void read(BackDeserializer deserializer) throws Throwable {
		int size = Array.getLength(array);
		for (int index = 0; index < size; index++) {
			if (ReadHelper.readOptional(deserializer.input, elementsWrapper.field.optional)) continue;

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
