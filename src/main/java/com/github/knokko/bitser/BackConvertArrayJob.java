package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.LegacyBitserException;
import com.github.knokko.bitser.legacy.BackBooleanValue;
import com.github.knokko.bitser.legacy.BackFloatValue;
import com.github.knokko.bitser.legacy.BackIntValue;

import java.lang.reflect.Array;

class BackConvertArrayJob {

	final Object legacyArray;
	final Object modernArray;
	final BitFieldWrapper modernWrapper;
	final RecursionNode node;

	BackConvertArrayJob(Object legacyArray, Object modernArray, BitFieldWrapper modernWrapper, RecursionNode node) {
		this.legacyArray = legacyArray;
		this.modernArray = modernArray;
		this.modernWrapper = modernWrapper;
		this.node = node;
	}

	void convert(BackDeserializer deserializer) {
		int length = Array.getLength(legacyArray);
		for (int index = 0; index < length; index++) {

			Object legacyElement = Array.get(legacyArray, index);
			if (legacyElement instanceof Boolean) legacyElement = BackBooleanValue.get((Boolean) legacyElement);
			if (legacyElement instanceof Character) legacyElement = new BackIntValue((Character) legacyElement);
			if (legacyElement instanceof Float) legacyElement = new BackFloatValue((Float) legacyElement);
			if (legacyElement instanceof Double) legacyElement = new BackFloatValue((Double) legacyElement);
			if (legacyElement instanceof Number) legacyElement = new BackIntValue(((Number) legacyElement).longValue());

			if (legacyElement == null) {
				if (modernWrapper.field.optional) continue;
				throw new LegacyBitserException("An element of " + modernWrapper.field + " is null, which is no longer allowed");
			}

			Object modernElement = modernWrapper.convert(deserializer, legacyElement, node, "elements");
			Array.set(modernArray, index, modernElement);
			if (modernWrapper.field.referenceTargetLabel != null) {
				deserializer.references.registerModern(legacyElement, modernElement);
			}
		}
	}
}
