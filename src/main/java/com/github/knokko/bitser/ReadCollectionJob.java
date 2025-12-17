package com.github.knokko.bitser;

import java.util.Collection;

public class ReadCollectionJob {

	final Collection<?> collection;
	final int size;
	final BitFieldWrapper elementsWrapper;
	final RecursionNode node;

	ReadCollectionJob(Collection<?> collection, int size, BitFieldWrapper elementsWrapper, RecursionNode node) {
		this.collection = collection;
		this.size = size;
		this.elementsWrapper = elementsWrapper;
		this.node = node;
	}

	void read(Deserializer deserializer) {
		Object[] elements = new Object[size];
		for (int index = 0; index < size; index++) {
			if (elementsWrapper.field.optional && !deserializer.input.read()) continue;

			elements[index] = elementsWrapper.read(deserializer);
			if (elementsWrapper.field.referenceTargetLabel != null) {
				deserializer.registerReferenceTarget(elementsWrapper.field.referenceTargetLabel, elements[index]);
			}
		}
		deserializer.populateCollectionJobs.add(new PopulateCollectionJob(collection, elements));
	}
}
