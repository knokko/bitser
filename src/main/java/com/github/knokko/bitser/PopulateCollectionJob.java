package com.github.knokko.bitser;

import java.util.Collection;
import java.util.Collections;

class PopulateCollectionJob {

	final Collection<?> collection;
	final Object[] elements;

	PopulateCollectionJob(Collection<?> collection, Object[] elements) {
		this.collection = collection;
		this.elements = elements;
	}

	void populate() {
		//noinspection unchecked
		Collections.addAll((Collection<Object>) collection, elements);
	}
}
