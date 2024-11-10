package com.github.knokko.bitser;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface BitEnum {

	Mode mode();

	enum Mode {
		Name,
		UniformOrdinal,
		VariableIntOrdinal
	}
}
