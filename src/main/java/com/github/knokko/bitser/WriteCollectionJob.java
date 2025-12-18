package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.InvalidBitValueException;

import java.util.Collection;

class WriteCollectionJob {

	final Collection<?> collection;
	final BitFieldWrapper elementsWrapper;
	final RecursionNode node;

	WriteCollectionJob(Collection<?> collection, BitFieldWrapper elementsWrapper, RecursionNode node) {
		this.collection = collection;
		this.elementsWrapper = elementsWrapper;
		this.node = node;
	}

	void write(Serializer serializer) throws Throwable {
		for (Object element : collection) {
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
