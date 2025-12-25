package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.LegacyBitserException;
import com.github.knokko.bitser.legacy.BackClassInstance;
import com.github.knokko.bitser.legacy.BackReference;
import com.github.knokko.bitser.legacy.BackStructInstance;
import com.github.knokko.bitser.util.RecursorException;

class BackConvertStructJob {

	final Object modernObject;
	final BitStructWrapper<?> modernInfo;
	final BackStructInstance legacyObject;
	final RecursionNode node;

	BackConvertStructJob(
			Object modernObject, BitStructWrapper<?> modernInfo,
			BackStructInstance legacyObject,
			RecursionNode node
	) {
		this.modernObject = modernObject;
		this.modernInfo = modernInfo;
		this.legacyObject = legacyObject;
		this.node = node;
	}

	void convert(BackDeserializer deserializer) {
		for (int hierarchyIndex = 0; hierarchyIndex < modernInfo.classHierarchy.size(); hierarchyIndex++) {
			SingleClassWrapper modernClass = modernInfo.classHierarchy.get(hierarchyIndex);
			BackClassInstance legacyValues = legacyObject.hierarchy[hierarchyIndex];
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
						} else throw new LegacyBitserException("Can't store legacy null in " + fieldName);
					}

					if (modernField.bitField instanceof ReferenceFieldWrapper) {
						if (legacyFieldValue instanceof BackReference) {
							deserializer.convertStructReferenceJobs.add(new BackConvertStructReferenceJob(
									modernObject, modernField.classField, ((BackReference) legacyFieldValue).reference,
									new RecursionNode(node, modernField.classField.getName())
							));
						} else {
							throw new LegacyBitserException("Can't store legacy " + legacyFieldValue + " in reference field");
						}
					} else {
						Object modernFieldValue = modernField.bitField.convert(
								deserializer, legacyFieldValue, node, fieldName
						);
						modernField.classField.set(modernObject, modernFieldValue);
						if (modernField.bitField.field.referenceTargetLabel != null) {
							deserializer.references.registerModern(legacyFieldValue, modernFieldValue);
							// TODO Test rename reference labels
							// TODO Test case where the legacy value is a reference target, but the modern value is not
							// TODO Test case where the legacy value is no reference target, but the modern value is
						}
					}
				} catch (Throwable failed) {
					throw new RecursorException(node.generateTrace(fieldName), failed);
				}
			}
		}
	}
}
