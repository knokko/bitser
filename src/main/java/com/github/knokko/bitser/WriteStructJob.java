package com.github.knokko.bitser;

import com.github.knokko.bitser.util.RecursorException;

class WriteStructJob {

	final Object structObject; // TODO Turn into array, to make it bulk write
	final BitStructWrapper<?> structInfo;
	final RecursionNode node;

	WriteStructJob(Object structObject, BitStructWrapper<?> structInfo, RecursionNode node) {
		this.structObject = structObject;
		this.structInfo = structInfo;
		this.node = node;
	}

	void write(Serializer serializer) {
		// TODO Debug context
		for (SingleClassWrapper structClass : structInfo.classHierarchy) {
			for (SingleClassWrapper.FieldWrapper field : structClass.getFields(false)) {
				try {
					Object value = field.classField.get(structObject);
					if (field.bitField.field.optional) {
						serializer.output.prepareProperty("optional", -1);
						serializer.output.write(value != null);
						serializer.output.finishProperty();
						if (value == null) continue;
					} else if (value == null) throw new UnsupportedOperationException("TODO");

					if (field.bitField instanceof ReferenceFieldWrapper) {
						serializer.structReferenceJobs.add(new WriteStructReferenceJob(
								value, ((ReferenceFieldWrapper) field.bitField).label,
								field.bitField instanceof StableReferenceFieldWrapper,
								new RecursionNode(node, field.classField.getName()))
						);
					} else {
						field.bitField.write(serializer, value, node, field.classField.getName());
						if (field.bitField.field.referenceTargetLabel != null) {
							serializer.references.registerTarget(field.bitField.field.referenceTargetLabel, value);
						}
					}
				} catch (Throwable failed) {
					throw new RecursorException(node.generateTrace(field.classField.getName()), failed);
				}
			}

			// TODO Do the same for functions
		}
	}
}
