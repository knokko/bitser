package com.github.knokko.bitser.field;

import com.github.knokko.bitser.BitStruct;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface IntegerField {

	int DIGIT_SIZE_TERMINATORS = 0;

	long minValue() default Long.MIN_VALUE;

	long maxValue() default Long.MAX_VALUE;

	boolean expectUniform();

	int digitSize() default DIGIT_SIZE_TERMINATORS;

	long[] commonValues() default {};

	@BitStruct(backwardCompatible = false)
	class Properties {

		@IntegerField(expectUniform = false, commonValues = {
				0L, -1L, Byte.MIN_VALUE, Short.MIN_VALUE, Integer.MIN_VALUE, Long.MIN_VALUE
		})
		public final long minValue;

		@IntegerField(expectUniform = true, commonValues = {
				Byte.MAX_VALUE, Short.MAX_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE
		})
		public final long maxValue;

		@BitField
		public final boolean expectUniform;

		@BitField
		@IntegerField(expectUniform = true, minValue = 0, maxValue = 7)
		public final int digitSize;

		@BitField
		@IntegerField(expectUniform = true)
		public final long[] commonValues;

		public Properties(long minValue, long maxValue, boolean expectUniform, int digitSize, long[] commonValues) {
			this.minValue = minValue;
			this.maxValue = maxValue;
			this.expectUniform = expectUniform;
			this.commonValues = commonValues;
			this.digitSize = digitSize;
		}

		public Properties(IntegerField field) {
			this(field.minValue(), field.maxValue(), field.expectUniform(), field.digitSize(), field.commonValues());
		}

		public Properties() {
			this(0, 0, false, 0, new long[0]);
		}
	}
}
