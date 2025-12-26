package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.RecursionException;

class WithStructJob {

	final Object structObject;
	final BitStructWrapper<?> structInfo;
	final RecursionNode node;

	WithStructJob(Object structObject, BitStructWrapper<?> structInfo, RecursionNode node) {
		this.structObject = structObject;
		this.structInfo = structInfo;
		this.node = node;
	}

	void register(AbstractReferenceTracker references) {
		for (SingleClassWrapper structClass : structInfo.classHierarchy) {
			for (SingleClassWrapper.FieldWrapper field : structClass.getFields(false)) {
				try {
					Object value = field.classField.get(structObject);
					if (value == null) continue;

					field.bitField.registerReferenceTargets(references, value, node, field.classField.getName());
					String label = field.bitField.field.referenceTargetLabel;
					if (label != null) references.registerTarget(label, value);
				} catch (Throwable failed) {
					throw new RecursionException(node.generateTrace(field.classField.getName()), failed);
				}
			}
		}
	}
}
