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

	IntegerField expectedIntegerMultiple() default @IntegerField(expectUniform = false);

	double[] commonValues() default {};

	@BitStruct(backwardCompatible = false)
	class Properties {

		@FloatField(commonValues = { 0.0, 0.01, 0.05, 0.1, 0.5, 1.0 })
		public final double expectMultipleOf;

		@FloatField(commonValues = { 0.0, 0.001, 0.01 })
		public final double errorTolerance;

		@BitField
		public final IntegerField.Properties expectedIntegerMultiple;

		@FloatField(commonValues = { 0.0, 1.0 })
		public final double[] commonValues;

		public Properties(
				double expectMultipleOf, double errorTolerance,
				IntegerField.Properties expectedIntegerMultiple, double[] commonValues
		) {
			this.expectMultipleOf = expectMultipleOf;
			this.errorTolerance = errorTolerance;
			this.expectedIntegerMultiple = expectedIntegerMultiple;
			this.commonValues = commonValues;
		}

		public Properties(FloatField field) {
			this(
					field.expectMultipleOf(), field.errorTolerance(),
					new IntegerField.Properties(field.expectedIntegerMultiple()),
					field.commonValues()
			);
		}

		public Properties() {
			this(0.0, 0.0, new IntegerField.Properties(), new double[0]);
		}
	}
}
