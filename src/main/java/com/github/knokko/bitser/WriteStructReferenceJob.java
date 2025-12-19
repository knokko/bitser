package com.github.knokko.bitser;

class WriteStructReferenceJob {

	final Object value;
	final ReferenceFieldWrapper fieldWrapper;
	final RecursionNode node;

	WriteStructReferenceJob(Object value, ReferenceFieldWrapper fieldWrapper, RecursionNode node) {
		this.value = value;
		this.fieldWrapper = fieldWrapper;
		this.node = node;
	}

	void save(Serializer serializer) throws Throwable {
		serializer.references.get(fieldWrapper).save(fieldWrapper, value, serializer.output);
	}
}
