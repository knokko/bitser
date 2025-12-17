package com.github.knokko.bitser;

class ReadStructJob {

	final Object structObject; // TODO Turn into array, to make it bulk read
	final BitStructWrapper<?> structInfo;
	final RecursionNode node;

	ReadStructJob(Object structObject, BitStructWrapper<?> structInfo, RecursionNode node) {
		this.structObject = structObject;
		this.structInfo = structInfo;
		this.node = node;
	}

	void read(Deserializer deserializer) {
		for (SingleClassWrapper structClass : structInfo.classHierarchy) {
			for (SingleClassWrapper.FieldWrapper field : structClass.getFields(false)) {
				if (field.bitField.field.optional && !deserializer.input.read()) continue;
				if (field.bitField instanceof ReferenceFieldWrapper) {
					deserializer.structReferenceJobs.add(new ReadStructReferenceJob(
							structObject, ((ReferenceFieldWrapper) field.bitField).label,
							field.bitField instanceof StableReferenceFieldWrapper, field.classField,
							new RecursionNode(node, field.classField.getName()))
					);
				} else {
					Object value = field.bitField.read(deserializer);
					field.classField.set(structObject, value);
					if (field.bitField.field.referenceTargetLabel != null) {
						deserializer.registerReferenceTarget(field.bitField.field.referenceTargetLabel, value);
					}
				}
			}
		}
	}
}
