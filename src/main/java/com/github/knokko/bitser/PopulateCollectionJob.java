package com.github.knokko.bitser;

import java.util.Collection;
import java.util.Collections;

class PopulateCollectionJob {

	final Collection<?> collection;
	final Object[] elements;
	final RecursionNode node;

	PopulateCollectionJob(Collection<?> collection, Object[] elements, RecursionNode node) {
		this.collection = collection;
		this.elements = elements;
		this.node = node;
	}

	void populate() {
		//noinspection unchecked
		Collections.addAll((Collection<Object>) collection, elements);
	}
}
