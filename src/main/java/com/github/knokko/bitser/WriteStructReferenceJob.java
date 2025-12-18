package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.ReferenceBitserException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

class WriteStructReferenceJob {

	final Object value;
	final String label;
	final boolean stable;
	final RecursionNode node;

	WriteStructReferenceJob(Object value, String label, boolean stable, RecursionNode node) {
		this.value = value;
		this.label = label;
		this.stable = stable;
		this.node = node;
	}

	void save(Serializer serializer) throws Throwable {
		if (stable) {
			BitStructWrapper<?> valueInfo = serializer.cache.getWrapperOrNull(value.getClass());
			if (valueInfo == null) {
				throw new InvalidBitFieldException("Can't find stable ID of " + value + " because it's not a BitStruct");
			}
			if (!valueInfo.hasStableId()) {
				throw new InvalidBitFieldException("Can't find StableReferenceFieldId of " + valueInfo);
			}
			UUID id = valueInfo.getStableId(value);
			IntegerBitser.encodeFullLong(id.getMostSignificantBits(), serializer.output);
			IntegerBitser.encodeFullLong(id.getLeastSignificantBits(), serializer.output);

			HashMap<UUID, Object> stableMap = serializer.references.stableTargets.get(label);
			if (stableMap == null) {
				throw new ReferenceBitserException("ehm");
			}
			Object actualValue = stableMap.get(id);
			if (actualValue != value) {
				throw new ReferenceBitserException("ehm");
			}
		} else {
			ArrayList<Object> references = serializer.references.unstableTargets.get(label);
			if (references == null) {
				throw new ReferenceBitserException("ehm");
			}
			// TODO Use a hash-code wrapper for this, and optimize
			// TODO Also validate that value was registered as target
			int index = references.indexOf(value);
			IntegerBitser.encodeUniformInteger(index, 0, references.size() - 1, serializer.output);
		}
	}
}
