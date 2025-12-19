package com.github.knokko.bitser;

import java.util.Collection;

class ReadCollectionReferenceJob {

	final Collection<?> collection;
	final int size;
	final ReferenceFieldWrapper elementsWrapper;
	final RecursionNode node;

	ReadCollectionReferenceJob(
			Collection<?> collection, int size,
			ReferenceFieldWrapper elementsWrapper,
			RecursionNode node
	) {
		this.collection = collection;
		this.size = size;
		this.elementsWrapper = elementsWrapper;
		this.node = node;
	}

	void resolve(Deserializer deserializer) throws Throwable {
		ReferenceTracker.LabelTargets targets = deserializer.references.get(elementsWrapper);

		Object[] elements = new Object[size];
		for (int index = 0; index < size; index++) {
			elements[index] = targets.get(elementsWrapper, deserializer.input);
		}

		deserializer.populateCollectionJobs.add(new PopulateCollectionJob(collection, elements, node));
	}
}
