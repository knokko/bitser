package com.github.knokko.bitser.field;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface StringField {

	IntegerField length() default @IntegerField(expectUniform = false);
}
