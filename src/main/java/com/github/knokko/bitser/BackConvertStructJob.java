package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.LegacyBitserException;
import com.github.knokko.bitser.legacy.LegacyClassValues;
import com.github.knokko.bitser.legacy.LegacyReference;
import com.github.knokko.bitser.legacy.LegacyStructInstance;
import com.github.knokko.bitser.exceptions.RecursionException;
import com.github.knokko.bitser.legacy.WithReference;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

record BackConvertStructJob(
		Object modernObject, BitStructWrapper<?> modernInfo,
		LegacyStructInstance legacyObject, RecursionNode node
) {

	private void convert(
			BackDeserializer deserializer, BitFieldWrapper field,
			boolean[] hasLegacyValues, Object[] legacyValues, Object[] modernValues,
			int id, String debugName, Object debugObject,
			Object modernObject, Field modernField
	) throws Exception {
		if (id >= hasLegacyValues.length || !hasLegacyValues[id]) {
			if (modernField != null) {
				if (modernField.get(modernObject) instanceof BitPostInit postInit) {
					var context = new BitPostInit.Context(
							deserializer.bitser, true,
							null, null, deserializer.withParameters
					);
					deserializer.postInitJobs.add(new PostInitJob(
							postInit, context, new RecursionNode(node, debugName)
					));
				}
			}
			return;
		}
		Object legacyValue = legacyValues[id];
		if (legacyValue == null) {
			if (field.field.optional) return;
			throw new LegacyBitserException(
					"Can't store legacy null in " + debugName + " for field " + field
			);
		}

		if (field instanceof ReferenceFieldWrapper) {
			if (legacyValue instanceof LegacyReference) {
				deserializer.convertStructReferenceJobs.add(new BackConvertStructReferenceJob(
						modernObject, modernField,
						modernValues, id,
						((LegacyReference) legacyValue).reference(),
						new RecursionNode(node, debugName)
				));
			} else if (legacyValue instanceof WithReference) {
				modernValues[id] = ((WithReference) legacyValue).reference();
			} else {
				throw new LegacyBitserException(
						"Can't store legacy " + legacyValue + " in reference " + debugObject
				);
			}
		} else {
			Object modernFieldValue = field.convert(
					deserializer, legacyValue, node, debugName
			);
			if (field.field.referenceTargetLabel != null) {
				deserializer.references.registerModern(legacyValue, modernFieldValue);
			}
			modernValues[id] = modernFieldValue;

		}
	}

	void convert(BackDeserializer deserializer) {
		if (legacyObject.hierarchy.length != modernInfo.classHierarchy.size()) {
			LegacyBitserException legacyException = new LegacyBitserException(
					"Class hierarchy size changed from " + legacyObject.hierarchy.length +
							" to " + modernInfo.classHierarchy.size()
			);
			throw new RecursionException(node.generateTrace(null), legacyException);
		}

		Map<Class<?>, Object[]> allModernValues = new HashMap<>();
		Map<Class<?>, Object[]> allLegacyValues = new HashMap<>();

		for (int hierarchyIndex = 0; hierarchyIndex < modernInfo.classHierarchy.size(); hierarchyIndex++) {
			SingleClassWrapper modernClass = modernInfo.classHierarchy.get(hierarchyIndex);
			LegacyClassValues legacyValues = legacyObject.hierarchy[hierarchyIndex];
			Object[] modernValues = new Object[legacyValues.values.length];

			for (var modernFunction : modernClass.functions) {
				String functionName = modernFunction.classMethod().getName();
				try {
					convert(
							deserializer, modernFunction.bitField(),
							legacyValues.hasValues, legacyValues.values, modernValues,
							modernFunction.id(), functionName, modernFunction.classMethod(),
							null, null
					);
				} catch (Throwable failed) {
					throw new RecursionException(node.generateTrace(functionName), failed);
				}
			}

			for (var modernField : modernClass.fields) {
				String fieldName = modernField.classField().getName();
				try {
					if (!modernField.readsMethodResult()) {
						convert(
								deserializer, modernField.bitField(),
								legacyValues.hasValues, legacyValues.values, modernValues,
								modernField.id(), fieldName, modernField.classField(),
								modernObject, modernField.classField()
						);
					}
					if (modernField.id() < legacyValues.hasValues.length && legacyValues.hasValues[modernField.id()]) {
						deserializer.methodReferenceToFieldJobs.add(new ReadStructMethodReferenceToFieldJob(
								modernValues, modernField.id(), modernObject,
								modernField.classField(), new RecursionNode(node, fieldName)
						));
					}
				} catch (Throwable failed) {
					throw new RecursionException(node.generateTrace(fieldName), failed);
				}
			}

			if (modernObject instanceof BitPostInit) {
				allLegacyValues.put(modernClass.myClass, legacyValues.values);
				allModernValues.put(modernClass.myClass, modernValues);
			}
		}

		if (modernObject instanceof BitPostInit) {
			var context = new BitPostInit.Context(
					deserializer.bitser, true, allModernValues,
					allLegacyValues, deserializer.withParameters
			);
			deserializer.postInitJobs.add(new PostInitJob((BitPostInit) modernObject, context, node));
		}
	}
}
