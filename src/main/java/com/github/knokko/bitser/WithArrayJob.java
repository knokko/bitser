package com.github.knokko.bitser;

import java.lang.reflect.Array;

class WithArrayJob {

	final Object array;
	final BitFieldWrapper elementsWrapper;
	final RecursionNode node;

	WithArrayJob(Object array, BitFieldWrapper elementsWrapper, RecursionNode node) {
		this.array = array;
		this.elementsWrapper = elementsWrapper;
		this.node = node;
	}

	void register(ReferenceTracker references) {
		int length = Array.getLength(array);
		for (int index = 0; index < length; index++) {
			Object element = Array.get(array, index);
			if (element == null) continue;

			elementsWrapper.registerReferenceTargets(references, element, node, "elements");
			if (elementsWrapper.field.referenceTargetLabel != null) {
				references.registerTarget(elementsWrapper.field.referenceTargetLabel, element);
			}
		}
	}
}
