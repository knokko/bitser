package com.github.knokko.bitser.field;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface StringField {

	boolean optional();

	IntegerField length() default @IntegerField(expectUniform = false);
}
