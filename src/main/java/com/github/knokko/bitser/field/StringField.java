package com.github.knokko.bitser.field;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface StringField {

	IntegerField length() default @IntegerField(expectUniform = false, minValue = 0, maxValue = Integer.MAX_VALUE);
}
