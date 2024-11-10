package com.github.knokko.bitser;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface BitEnum {

	Mode mode();

	enum Mode {
		Name,
		UniformOrdinal,
		VariableIntOrdinal
	}
}