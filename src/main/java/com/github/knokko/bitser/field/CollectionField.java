package com.github.knokko.bitser.field;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface CollectionField {

	IntegerField size() default @IntegerField(expectUniform = false);

	String valueAnnotations();
}
