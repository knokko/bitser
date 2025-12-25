package com.github.knokko.bitser;

import java.lang.reflect.Array;

class WriteArrayReferenceJob {

	final Object array;
	final ReferenceFieldWrapper elementsWrapper;
	final String nullErrorMessage;
	final RecursionNode node;

	WriteArrayReferenceJob(
			Object array, ReferenceFieldWrapper elementsWrapper,
			String nullErrorMessage, RecursionNode node
	) {
		this.array = array;
		this.elementsWrapper = elementsWrapper;
		this.nullErrorMessage = nullErrorMessage;
		this.node = node;
	}

	void save(Serializer serializer) throws Throwable {
		int length = Array.getLength(array);
		for (int index = 0; index < length; index++) {
			Object reference = Array.get(array, index);
			if (WriteHelper.writeOptional(
					serializer.output, reference, elementsWrapper.field.optional, nullErrorMessage)
			) continue;
			serializer.references.save(elementsWrapper, reference, serializer.output);
		}
	}
}
