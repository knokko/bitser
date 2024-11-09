package com.github.knokko.bitser.field;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface FloatField {

	double expectMultipleOf() default 0.0;

	double errorTolerance() default 0.001;
}
