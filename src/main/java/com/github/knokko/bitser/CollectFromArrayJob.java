package com.github.knokko.bitser;

import java.lang.reflect.Array;

record CollectFromArrayJob(Object array, BitFieldWrapper elementsWrapper, RecursionNode node, String description) {

	void collect(InstanceCollector collector) {
		if (elementsWrapper instanceof ReferenceFieldWrapper) return;
		int length = Array.getLength(array);

		for (int index = 0; index < length; index++) {
			Object element = Array.get(array, index);
			if (element == null) continue;
			collector.register(element);
			elementsWrapper.collectInstances(collector, element, node, description);
		}
	}
}
