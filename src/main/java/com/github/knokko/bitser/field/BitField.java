package com.github.knokko.bitser.field;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BitField {

	int ordering();

	boolean optional() default false;

	int since() default -1;

	class Properties {

		public final int ordering;
		public final boolean optional;
		public final Class<?> type;

		public Properties(int ordering, boolean optional, Class<?> type) {
			this.ordering = ordering;
			this.optional = optional;
			this.type = type;
		}
	}
}
