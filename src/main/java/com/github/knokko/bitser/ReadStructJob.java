package com.github.knokko.bitser;

import com.github.knokko.bitser.util.RecursorException;

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
			Object[] functionValues;
			if (structClass.functions.isEmpty()) functionValues = new Object[0];
			else functionValues = new Object[structClass.functions.get(structClass.functions.size() - 1).id + 1];

			for (SingleClassWrapper.FieldWrapper field : structClass.getFields(false)) {
				try {
					if (field.bitField.field.optional && !deserializer.input.read()) continue;
					if (field.bitField instanceof ReferenceFieldWrapper) {
						deserializer.structReferenceJobs.add(new ReadStructReferenceJob(
								structObject, field.classField, (ReferenceFieldWrapper) field.bitField,
								new RecursionNode(node, field.classField.getName()))
						);
					} else {
						Object value = field.bitField.read(deserializer, node, field.classField.getName());
						field.classField.set(structObject, value);
						if (field.bitField.field.referenceTargetLabel != null) {
							deserializer.references.registerTarget(field.bitField.field.referenceTargetLabel, value);
						}
					}
				} catch (Throwable failed) {
					throw new RecursorException(node.generateTrace(field.classField.getName()), failed);
				}
			}

			for (SingleClassWrapper.FunctionWrapper function : structClass.functions) {
				try {
					if (function.bitField.field.optional && !deserializer.input.read()) continue;
					if (function.bitField instanceof ReferenceFieldWrapper) {
						throw new UnsupportedOperationException("TODO");
//						deserializer.structReferenceJobs.add(new ReadStructReferenceJob(
//								structObject, ((ReferenceFieldWrapper) field.bitField).label,
//								field.bitField instanceof StableReferenceFieldWrapper, field.classField,
//								new RecursionNode(node, field.classField.getName()))
//						);
					} else {
						Object value = function.bitField.read(deserializer, node, function.classMethod.getName());
						// TODO Save somewhere
//						field.classField.set(structObject, value);
//						if (field.bitField.field.referenceTargetLabel != null) {
//							deserializer.references.registerTarget(field.bitField.field.referenceTargetLabel, value);
//						}
					}
				} catch (Throwable failed) {
					throw new RecursorException(node.generateTrace(function.classMethod.getName()), failed);
				}
			}
		}
	}
}
