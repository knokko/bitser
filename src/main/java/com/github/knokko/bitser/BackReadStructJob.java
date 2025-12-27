package com.github.knokko.bitser;

import com.github.knokko.bitser.legacy.LegacyClassValues;
import com.github.knokko.bitser.legacy.LegacyStructInstance;
import com.github.knokko.bitser.legacy.LegacyUUIDValue;
import com.github.knokko.bitser.exceptions.RecursionException;

import java.util.ArrayList;

record BackReadStructJob(LegacyStructInstance legacyObject, LegacyStruct legacyInfo, RecursionNode node) {

	private void readFieldsOrFunctions(
			BackDeserializer deserializer, ArrayList<LegacyField> fieldsOrFunctions, String namePrefix,
			boolean[] hasValues, Object[] values
	) {
		for (LegacyField fieldOrFunction : fieldsOrFunctions) {
			String fieldOrFunctionName = namePrefix + fieldOrFunction.id;
			try {
				hasValues[fieldOrFunction.id] = true;
				if (ReadHelper.readOptional(deserializer.input, fieldOrFunction.bitField.field.optional)) {
					continue;
				}

				if (fieldOrFunction.bitField instanceof ReferenceFieldWrapper) {
					deserializer.structReferenceJobs.add(new BackReadStructReferenceJob(
							values, fieldOrFunction.id, (ReferenceFieldWrapper) fieldOrFunction.bitField,
							new RecursionNode(node, fieldOrFunctionName)
					));
				} else {
					deserializer.input.pushContext(node, fieldOrFunctionName);
					Object value = fieldOrFunction.bitField.read(deserializer, node, fieldOrFunctionName);
					deserializer.input.popContext(node, fieldOrFunctionName);

					values[fieldOrFunction.id] = value;
					if (fieldOrFunction.bitField instanceof UUIDFieldWrapper &&
							((UUIDFieldWrapper) fieldOrFunction.bitField).isStableReferenceId
					) {
						legacyObject.stableID = ((LegacyUUIDValue) value).value();
					}

					if (fieldOrFunction.bitField.field.referenceTargetLabel != null) {
						deserializer.references.registerLegacyTarget(
								fieldOrFunction.bitField.field.referenceTargetLabel, value
						);
					}
				}
			} catch (Throwable failed) {
				throw new RecursionException(node.generateTrace(fieldOrFunctionName), failed);
			}
		}
	}

	void read(BackDeserializer deserializer) {
		for (int hierarchyIndex = 0; hierarchyIndex < legacyInfo.classHierarchy.size(); hierarchyIndex++) {
			LegacyClass legacyClass = legacyInfo.classHierarchy.get(hierarchyIndex);
			LegacyClassValues legacyClassInstance = legacyObject.hierarchy[hierarchyIndex];

			readFieldsOrFunctions(
					deserializer, legacyClass.fields, "field ",
					legacyClassInstance.hasFieldValues, legacyClassInstance.fieldValues
			);
			readFieldsOrFunctions(
					deserializer, legacyClass.functions, "function ",
					legacyClassInstance.hasFunctionValues, legacyClassInstance.functionValues
			);
		}
	}
}
