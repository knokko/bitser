package com.github.knokko.bitser;

import java.lang.reflect.Array;

class WriteArrayJob {

	final Object array;
	final BitFieldWrapper elementsWrapper;
	final RecursionNode node;

	WriteArrayJob(Object array, BitFieldWrapper elementsWrapper, RecursionNode node) {
		this.array = array;
		this.elementsWrapper = elementsWrapper;
		this.node = node;
	}

	void write(Serializer serializer) throws Throwable {
		int length = Array.getLength(array);
		for (int index = 0; index < length; index++) {
			Object element = Array.get(array, index);
			if (WriteHelper.writeOptional(
					serializer.output, element, elementsWrapper.field.optional,
					"array must not have null elements"
			)) continue;

			elementsWrapper.write(serializer, element, node, "elements");
			if (elementsWrapper.field.referenceTargetLabel != null) {
				serializer.references.registerTarget(elementsWrapper.field.referenceTargetLabel, element);
			}
		}
	}
}
