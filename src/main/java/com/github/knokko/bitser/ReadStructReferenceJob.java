package com.github.knokko.bitser;

import java.lang.reflect.Field;

class ReadStructReferenceJob {

	final Object structObject;
	final Field classField;
	final ReferenceFieldWrapper fieldWrapper;
	final RecursionNode node;

	ReadStructReferenceJob(Object structObject, Field classField, ReferenceFieldWrapper fieldWrapper, RecursionNode node) {
		this.structObject = structObject;
		this.classField = classField;
		this.fieldWrapper = fieldWrapper;
		this.node = node;
	}

	void resolve(Deserializer deserializer) throws Throwable {
		classField.set(structObject, deserializer.references.get(fieldWrapper).get(fieldWrapper, deserializer.input));
	}
}
