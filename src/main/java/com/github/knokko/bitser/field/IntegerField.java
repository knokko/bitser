package com.github.knokko.bitser.field;

import com.github.knokko.bitser.BitStruct;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface IntegerField {

	long minValue() default Long.MIN_VALUE;

	long maxValue() default Long.MAX_VALUE;

	boolean expectUniform();

	@BitStruct(backwardCompatible = false)
	class Properties {

		@IntegerField(expectUniform = true)
		public final long minValue;

		@IntegerField(expectUniform = true)
		public final long maxValue;

		@BitField
		public final boolean expectUniform;

		public Properties(long minValue, long maxValue, boolean expectUniform) {
			this.minValue = minValue;
			this.maxValue = maxValue;
			this.expectUniform = expectUniform;
		}

		public Properties(IntegerField field) {
			this(field.minValue(), field.maxValue(), field.expectUniform());
		}

		public Properties() {
			this(0, 0, false);
		}
	}
}
