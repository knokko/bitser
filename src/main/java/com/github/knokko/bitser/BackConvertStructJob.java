package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.LegacyBitserException;
import com.github.knokko.bitser.legacy.LegacyClassValues;
import com.github.knokko.bitser.legacy.LegacyReference;
import com.github.knokko.bitser.legacy.LegacyStructInstance;
import com.github.knokko.bitser.exceptions.RecursionException;

import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.max;

record BackConvertStructJob(
		Object modernObject, BitStructWrapper<?> modernInfo,
		LegacyStructInstance legacyObject, RecursionNode node
) {

	void convert(BackDeserializer deserializer) {
		if (legacyObject.hierarchy.length != modernInfo.classHierarchy.size()) {
			LegacyBitserException legacyException = new LegacyBitserException(
					"Class hierarchy size changed from " + legacyObject.hierarchy.length +
							" to " + modernInfo.classHierarchy.size()
			);
			throw new RecursionException(node.generateTrace(null), legacyException);
		}

		Map<Class<?>, Object[]> legacyFieldValues = new HashMap<>();
		Map<Class<?>, Object[]> legacyFunctionValues = new HashMap<>();
		Map<Class<?>, Object[]> modernFunctionValues = new HashMap<>();

		for (int hierarchyIndex = 0; hierarchyIndex < modernInfo.classHierarchy.size(); hierarchyIndex++) {
			SingleClassWrapper modernClass = modernInfo.classHierarchy.get(hierarchyIndex);
			LegacyClassValues legacyValues = legacyObject.hierarchy[hierarchyIndex];

			for (SingleClassWrapper.FieldWrapper modernField : modernClass.fields) {
				String fieldName = modernField.classField.getName();
				try {
					if (modernField.id >= legacyValues.hasFieldValues.length ||
							!legacyValues.hasFieldValues[modernField.id]
					) continue;

					Object legacyFieldValue = legacyValues.fieldValues[modernField.id];
					if (legacyFieldValue == null) {
						if (modernField.bitField.field.optional) {
							modernField.classField.set(modernObject, null);
							continue;
						} else throw new LegacyBitserException(
								"Can't store legacy null in " + fieldName + " for field " + modernField
						);
					}

					if (modernField.bitField instanceof ReferenceFieldWrapper) {
						if (legacyFieldValue instanceof LegacyReference) {
							deserializer.convertStructReferenceJobs.add(new BackConvertStructReferenceJob(
									modernObject, modernField.classField, ((LegacyReference) legacyFieldValue).reference(),
									new RecursionNode(node, modernField.classField.getName())
							));
						} else if (legacyFieldValue instanceof WithReference) {
							modernField.classField.set(modernObject, ((WithReference) legacyFieldValue).reference);
						} else {
							throw new LegacyBitserException(
									"Can't store legacy " + legacyFieldValue +
											" in reference field " + modernField.classField
							);
						}
					} else {
						Object modernFieldValue = modernField.bitField.convert(
								deserializer, legacyFieldValue, node, fieldName
						);
						modernField.classField.set(modernObject, modernFieldValue);
						if (modernField.bitField.field.referenceTargetLabel != null) {
							deserializer.references.registerModern(legacyFieldValue, modernFieldValue);
						}
					}
				} catch (Throwable failed) {
					throw new RecursionException(node.generateTrace(fieldName), failed);
				}
			}

			if (modernObject instanceof BitPostInit) {
				int largestModernFunctionID = -1;
				for (SingleClassWrapper.FunctionWrapper candidateFunction : modernClass.functions) {
					largestModernFunctionID = max(largestModernFunctionID, candidateFunction.id);
				}
				SingleClassWrapper.FunctionWrapper[] modernFunctions = new SingleClassWrapper.FunctionWrapper[
						1 + largestModernFunctionID
						];
				for (SingleClassWrapper.FunctionWrapper modernFunction : modernClass.functions) {
					modernFunctions[modernFunction.id] = modernFunction;
				}

				Object[] currentModernFunctionValues = new Object[
						max(modernFunctions.length, legacyValues.functionValues.length)
						];

				legacyFieldValues.put(modernClass.myClass, legacyValues.fieldValues);
				legacyFunctionValues.put(modernClass.myClass, legacyValues.functionValues);
				modernFunctionValues.put(modernClass.myClass, currentModernFunctionValues);

				for (int functionID = 0; functionID < legacyValues.functionValues.length; functionID++) {
					if (!legacyValues.hasFunctionValues[functionID]) continue;
					Object legacyFunctionValue = legacyValues.functionValues[functionID];

					SingleClassWrapper.FunctionWrapper modernFunction = null;
					if (functionID < modernFunctions.length) modernFunction = modernFunctions[functionID];

					if (modernFunction == null) {
						if (legacyFunctionValue instanceof LegacyReference) {
							deserializer.convertStructFunctionReferenceJobs.add(new BackConvertStructFunctionReferenceJob(
									currentModernFunctionValues, functionID,
									((LegacyReference) legacyFunctionValue).reference(),
									new RecursionNode(node, "legacy function " + functionID)
							));
						} else {
							currentModernFunctionValues[functionID] = legacyFunctionValue;
						}
					} else {
						try {
							if (legacyFunctionValue == null) {
								if (modernFunction.bitField.field.optional) continue;
								else throw new LegacyBitserException(
										"Can't store legacy null for " + modernFunction.classMethod.getName() +
												" for function " + modernFunction
								);
							}

							if (modernFunction.bitField instanceof ReferenceFieldWrapper) {
								if (legacyFunctionValue instanceof LegacyReference) {
									deserializer.convertStructFunctionReferenceJobs.add(new BackConvertStructFunctionReferenceJob(
											currentModernFunctionValues, modernFunction.id,
											((LegacyReference) legacyFunctionValue).reference(),
											new RecursionNode(node, modernFunction.classMethod.getName())
									));
								} else {
									throw new LegacyBitserException(
											"Can't store legacy " + legacyFunctionValue +
													" as result of reference function " + modernFunction.classMethod
									);
								}
							} else {
								Object modernFunctionValue = modernFunction.bitField.convert(
										deserializer, legacyFunctionValue, node, modernFunction.classMethod.getName()
								);
								currentModernFunctionValues[modernFunction.id] = modernFunctionValue;
							}
						} catch (Throwable failed) {
							throw new RecursionException(node.generateTrace(modernFunction.classMethod.getName()), failed);
						}
					}
				}
			}
		}

		if (modernObject instanceof BitPostInit) {
			BitPostInit.Context context = new BitPostInit.Context(
					deserializer.bitser, true, modernFunctionValues,
					legacyFieldValues, legacyFunctionValues, deserializer.withParameters
			);
			deserializer.postInitJobs.add(new PostInitJob((BitPostInit) modernObject, context, node));
		}
	}
}
