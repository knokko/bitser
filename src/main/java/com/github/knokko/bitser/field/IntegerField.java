package com.github.knokko.bitser.field;

import com.github.knokko.bitser.BitStruct;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 *     This annotation is required to specify how an integer field should be serialized. Note that the type of the
 *     field does not necessarily have to be <b>int</b>: it can also be another integer type like <b>long</b> or
 *     {@link Short}.
 * </p>
 * <p>
 *     The easiest way to annotate an integer field is by using either {@code @IntegerField(expectUniform=false)} or
 *     {@code @IntegerField(expectUniform=true)}, but bitser supports a couple of more properties to potentially save
 *     storage space.
 * </p>
 *
 * <h3>Backward compatibility</h3>
 * <p>
 *     If a field of the original serialized struct is an {@code IntegerField}, the corresponding field of the new
 *     deserialized struct must also be an {@code IntegerField}. (At least, if a corresponding field exists. If not, the
 *     old value is simply discarded.)
 * </p>
 * <p>
 *     You are allowed to change the <i>type</i> of the field, but only if the new type can represent all the values
 *     that were serialized. (For instance, changing the type from <b>int</b> to <b>byte</b> is only allowed if all
 *     serialized values were in the range `[Byte.MIN_VALUE, Byte.MAX_VALUE]`. Changing the type to a larger type
 *     (e.g. <b>int</b> to <b>long</b>) is always allowed.
 * </p>
 * <p>
 *     Likewise, you are allowed to change {@link #minValue()} and {@link #maxValue()}, but only if all the original
 *     serialized values were in the range `[minValue, maxValue]`.
 * </p>
 * <p>
 *     You can safely change the other properties: {@link #expectUniform()}, {@link #digitSize()},
 *     and {@link #commonValues()}
 * </p>
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface IntegerField {

	/**
	 * The 'magic' value of {@link #digitSize()} to indicate that bitser should use its terminator-bit strategy instead
	 * of a digit-based serialization strategy. This TODO
	 */
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
