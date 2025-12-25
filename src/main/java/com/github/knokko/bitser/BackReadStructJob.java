package com.github.knokko.bitser;

import com.github.knokko.bitser.legacy.BackClassInstance;
import com.github.knokko.bitser.legacy.BackStructInstance;
import com.github.knokko.bitser.legacy.BackUUIDValue;
import com.github.knokko.bitser.util.RecursorException;

class BackReadStructJob {

	final BackStructInstance legacyObject;
	final LegacyStruct legacyInfo;
	final RecursionNode node;

	BackReadStructJob(BackStructInstance legacyObject, LegacyStruct legacyInfo, RecursionNode node) {
		this.legacyObject = legacyObject;
		this.legacyInfo = legacyInfo;
		this.node = node;
	}

	void read(BackDeserializer deserializer) {
		for (int hierarchyIndex = 0; hierarchyIndex < legacyInfo.classHierarchy.size(); hierarchyIndex++) {
			LegacyClass legacyClass = legacyInfo.classHierarchy.get(hierarchyIndex);
			BackClassInstance legacyClassInstance = legacyObject.hierarchy[hierarchyIndex];

			for (LegacyField legacyField : legacyClass.fields) {
				String fieldName = "field " + legacyField.id;
				try {
					legacyClassInstance.hasFieldValues[legacyField.id] = true;
					if (ReadHelper.readOptional(deserializer.input, legacyField.bitField.field.optional)) {
						continue;
					}

					if (legacyField.bitField instanceof ReferenceFieldWrapper) {
						deserializer.structReferenceJobs.add(new BackReadStructReferenceJob(
								legacyClassInstance.fieldValues, legacyField.id,
								(ReferenceFieldWrapper) legacyField.bitField, new RecursionNode(node, fieldName)
						));
					} else {
						deserializer.input.pushContext(node, fieldName);
						Object value = legacyField.bitField.read(deserializer, node, fieldName);
						deserializer.input.popContext(node, fieldName);

						legacyClassInstance.fieldValues[legacyField.id] = value;
						if (legacyField.bitField instanceof UUIDFieldWrapper &&
								((UUIDFieldWrapper) legacyField.bitField).isStableReferenceId
						) {
							legacyObject.stableID = ((BackUUIDValue) value).value;
						}

						if (legacyField.bitField.field.referenceTargetLabel != null) {
							deserializer.references.registerLegacyTarget(
									legacyField.bitField.field.referenceTargetLabel, value
							);
						}
					}
				} catch (Throwable failed) {
					throw new RecursorException(node.generateTrace(fieldName), failed);
				}
			}

			// TODO Try code reuse
			for (LegacyField legacyFunction : legacyClass.functions) {
				String functionName = "function " + legacyFunction.id;
				try {
					legacyClassInstance.hasFunctionValues[legacyFunction.id] = true;
					if (ReadHelper.readOptional(deserializer.input, legacyFunction.bitField.field.optional)) {
						continue;
					}

					if (legacyFunction.bitField instanceof ReferenceFieldWrapper) {
						deserializer.structReferenceJobs.add(new BackReadStructReferenceJob(
								legacyClassInstance.functionValues, legacyFunction.id,
								(ReferenceFieldWrapper) legacyFunction.bitField, new RecursionNode(node, functionName)
						));
					} else {
						deserializer.input.pushContext(node, functionName);
						Object value = legacyFunction.bitField.read(deserializer, node, functionName);
						deserializer.input.popContext(node, functionName);

						legacyClassInstance.functionValues[legacyFunction.id] = value;
						if (legacyFunction.bitField instanceof UUIDFieldWrapper &&
								((UUIDFieldWrapper) legacyFunction.bitField).isStableReferenceId
						) {
							legacyObject.stableID = ((BackUUIDValue) value).value;
						}
					}
				} catch (Throwable failed) {
					throw new RecursorException(node.generateTrace(functionName), failed);
				}
			}
		}

//		if (structObject instanceof BitPostInit) {
//			BitPostInit.Context context = new BitPostInit.Context(
//					deserializer.bitser, false, serializedFunctionValues,
//					null, null, deserializer.withParameters
//			);
//			deserializer.postInitJobs.add(new PostInitJob((BitPostInit) structObject, context));
//		}
	}
}
