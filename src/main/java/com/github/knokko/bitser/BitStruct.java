package com.github.knokko.bitser;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface BitStruct {

    boolean backwardCompatible();
}
