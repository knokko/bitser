package com.github.knokko.bitser;

record DeepCompareReferenceJob(Object referenceA, Object referenceB) {

	boolean notEqual(DeepComparator comparator) {
		// TODO Check out IdentityHashMap
		var mappedA = comparator.referenceTargetMapping.get(new ReferenceTracker.IdentityWrapper(referenceA));
		if (mappedA == null) return referenceA != referenceB;
		else return mappedA != referenceB;
	}
}
