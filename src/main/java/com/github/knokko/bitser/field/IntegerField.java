package com.github.knokko.bitser.field;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface IntegerField {

    long minValue() default Long.MIN_VALUE;

    long maxValue() default Long.MAX_VALUE;

    boolean expectUniform();
}
