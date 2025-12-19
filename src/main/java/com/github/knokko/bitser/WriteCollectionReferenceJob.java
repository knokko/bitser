package com.github.knokko.bitser;

import java.util.Collection;

class WriteCollectionReferenceJob {

	final Collection<?> collection;
	final ReferenceFieldWrapper elementsWrapper;
	final RecursionNode node;

	WriteCollectionReferenceJob(Collection<?> collection, ReferenceFieldWrapper elementsWrapper, RecursionNode node) {
		this.collection = collection;
		this.elementsWrapper = elementsWrapper;
		this.node = node;
	}

	void save(Serializer serializer) throws Throwable {
		ReferenceTracker.LabelTargets targets = serializer.references.get(elementsWrapper);
		for (Object element : collection) targets.save(elementsWrapper, element, serializer.output);
	}
}
