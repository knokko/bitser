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
			//SingleClassWrapper modernClass = modernInfo.classHierarchy.get(hierarchyIndex);
			BackClassInstance legacyClassInstance = legacyObject.hierarchy[hierarchyIndex];
//			Object[] functionValues;
//			if (structClass.functions.isEmpty()) functionValues = new Object[0];
//			else functionValues = new Object[structClass.functions.get(structClass.functions.size() - 1).id + 1];

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
//			for (SingleClassWrapper.FunctionWrapper function : structClass.functions) {
//				try {
//					if (ReadHelper.readOptional(deserializer.input, function.bitField.field.optional)) continue;
//					if (function.bitField instanceof ReferenceFieldWrapper) {
//						throw new InvalidBitFieldException("Bit functions returning references are not supported");
//					} else {
//						String methodName = function.classMethod.getName();
//						deserializer.input.pushContext(node, methodName);
//						functionValues[function.id] = function.bitField.read(deserializer, node, methodName);
//						deserializer.input.popContext(node, methodName);
//
//
//						if (function.bitField.field.referenceTargetLabel != null) {
//							throw new InvalidBitFieldException(
//									"Bit functions returning reference targets are not supported"
//							);
//						}
//					}
//				} catch (Throwable failed) {
//					throw new RecursorException(node.generateTrace(function.classMethod.getName()), failed);
//				}
//			}
//
//			if (structObject instanceof BitPostInit) {
//				serializedFunctionValues.put(structClass.myClass, functionValues);
//			}
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
