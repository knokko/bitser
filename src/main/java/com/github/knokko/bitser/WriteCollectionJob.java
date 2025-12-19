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
			if (WriteHelper.writeOptional(
					serializer.output, element, elementsWrapper.field.optional,
					"collection must not have null elements"
			)) continue;

			elementsWrapper.write(serializer, element, node, "elements");
			if (elementsWrapper.field.referenceTargetLabel != null) {
				serializer.references.registerTarget(elementsWrapper.field.referenceTargetLabel, element);
			}
		}
	}
}
