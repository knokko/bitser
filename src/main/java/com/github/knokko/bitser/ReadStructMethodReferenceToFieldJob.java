package com.github.knokko.bitser;

import java.lang.reflect.Field;

record ReadStructMethodReferenceToFieldJob(
		Object[] functionValues, int functionID,
		Object structObject, Field classField,
		RecursionNode node
) {
	void resolve() throws Exception {
		classField.set(structObject, functionValues[functionID]);
	}
}
