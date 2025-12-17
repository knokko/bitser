package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.ReferenceBitserException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

class ReadCollectionReferenceJob {

	final Collection<?> collection;
	final int size;
	final String label;
	final boolean stable;
	final boolean optional;
	final RecursionNode node;

	ReadCollectionReferenceJob(
			Collection<?> collection, int size, String label,
			boolean stable, boolean optional,
			RecursionNode node
	) {
		this.collection = collection;
		this.size = size;
		this.label = label;
		this.stable = stable;
		this.optional = optional;
		this.node = node;
	}

	void resolve(Deserializer deserializer) throws Throwable {
		HashMap<UUID, Object> stableMap = null;
		ArrayList<Object> unstableList = null;
		if (stable) {
			stableMap = deserializer.stableReferenceTargets.get(label);
			if (stableMap == null) {
				throw new ReferenceBitserException("ehm");
			}
		} else {
			unstableList = deserializer.unstableReferenceTargets.get(label);
			if (unstableList == null) {
				throw new ReferenceBitserException("ehm");
			}
		}

		Object[] elements = new Object[size];
		for (int index = 0; index < size; index++) {
			if (optional && !deserializer.input.read()) continue;

			Object value;
			if (stable) {
				UUID id = new UUID(
						IntegerBitser.decodeFullLong(deserializer.input),
						IntegerBitser.decodeFullLong(deserializer.input)
				);
				value = stableMap.get(id);
				if (value == null) {
					throw new ReferenceBitserException("ehm");
				}

			} else {
				int referenceIndex = (int) IntegerBitser.decodeUniformInteger(
						0, unstableList.size() - 1, deserializer.input
				);
				value = unstableList.get(referenceIndex);
			}

			elements[index] = value;
		}

		deserializer.populateCollectionJobs.add(new PopulateCollectionJob(collection, elements, node));
	}
}
