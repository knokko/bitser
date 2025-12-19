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
					String fieldName = field.classField.getName();
					if (ReadHelper.readOptional(deserializer.input, field.bitField.field.optional)) continue;
					if (field.bitField instanceof ReferenceFieldWrapper) {
						deserializer.structReferenceJobs.add(new ReadStructReferenceJob(
								structObject, field.classField, (ReferenceFieldWrapper) field.bitField,
								new RecursionNode(node, fieldName))
						);
					} else {
						deserializer.input.pushContext(node, fieldName);
						Object value = field.bitField.read(deserializer, node, fieldName);
						deserializer.input.popContext(node, fieldName);

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
					if (ReadHelper.readOptional(deserializer.input, function.bitField.field.optional)) continue;
					if (function.bitField instanceof ReferenceFieldWrapper) {
						throw new UnsupportedOperationException("TODO");
//						deserializer.structReferenceJobs.add(new ReadStructReferenceJob(
//								structObject, ((ReferenceFieldWrapper) field.bitField).label,
//								field.bitField instanceof StableReferenceFieldWrapper, field.classField,
//								new RecursionNode(node, field.classField.getName()))
//						);
					} else {
						String methodName = function.classMethod.getName();
						deserializer.input.pushContext(node, methodName);
						Object value = function.bitField.read(deserializer, node, methodName);
						deserializer.input.popContext(node, methodName);
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
