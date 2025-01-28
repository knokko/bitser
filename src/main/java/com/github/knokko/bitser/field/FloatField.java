package com.github.knokko.bitser.field;

import com.github.knokko.bitser.BitStruct;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface FloatField {

	double expectMultipleOf() default 0.0;

	double errorTolerance() default 0.001;

	@BitStruct(backwardCompatible = false)
	class Properties {

		@FloatField
		public final double expectMultipleOf;

		@FloatField
		public final double errorTolerance;

		public Properties(double expectMultipleOf, double errorTolerance) {
			this.expectMultipleOf = expectMultipleOf;
			this.errorTolerance = errorTolerance;
		}

		public Properties(FloatField field) {
			this(field.expectMultipleOf(), field.errorTolerance());
		}

		public Properties() {
			this(0.0, 0.0);
		}
	}
}
