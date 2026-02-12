package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.RecursionException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;

class DeepComparator {

	final Bitser bitser;

	final ArrayList<DeepCompareStructsJob> structJobs = new ArrayList<>();
	final ArrayList<DeepCompareArraysJob> arrayJobs = new ArrayList<>();
	final IdentityHashMap<Object, Object> referenceTargetMapping = new IdentityHashMap<>();
	final ArrayList<DeepCompareReferenceJob> referenceJobs = new ArrayList<>();

	DeepComparator(Object rootA, Object rootB, BitStructWrapper<?> rootWrapper, Bitser bitser) {
		this.bitser = bitser;
		this.structJobs.add(new DeepCompareStructsJob(rootA, rootB, rootWrapper, new RecursionNode("root")));
	}

	boolean equals() {
		while (!structJobs.isEmpty() || !arrayJobs.isEmpty()) {
			if (!structJobs.isEmpty()) {
				if (structJobs.remove(structJobs.size() - 1).certainlyNotEqual(this)) return false;
			}
			if (!arrayJobs.isEmpty()) {
				var job = arrayJobs.remove(arrayJobs.size() - 1);
				try {
					if (job.certainlyNotEqual(this)) return false;
				} catch (Throwable failed) {
					throw new RecursionException(job.node().generateTrace(job.description()), failed);
				}
			}
		}

		for (var job : referenceJobs) {
			if (job.notEqual(this)) return false;
		}

		return true;
	}
}
