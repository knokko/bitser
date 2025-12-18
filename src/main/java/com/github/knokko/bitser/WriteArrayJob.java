package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.InvalidBitValueException;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Objects;

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
			if (elementsWrapper.field.optional) {
				serializer.output.write(element != null);
				if (element == null) continue;
			} else if (element == null) {
				throw new InvalidBitValueException("null");
			}

			elementsWrapper.write(serializer, element, node, "elements");
			if (elementsWrapper.field.referenceTargetLabel != null) {
				serializer.references.registerTarget(elementsWrapper.field.referenceTargetLabel, element);
			}
		}
	}
}
