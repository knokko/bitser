package com.github.knokko.bitser.field;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 *     This annotation can be used to specify how a string field should be serialized.
 * </p>
 *
 * <p>
 *     This annotation is optional when a field is already annotated with {@link BitField}. When this annotation is
 *     omitted, the string will be serialized using the default settings.
 * </p>
 *
 * <p>
 *     Currently, only the {@link #length()} setting can be changed, but bitser may support more settings in
 *     the future.
 * </p>
 *
 * <h3>Backward compatibility</h3>
 * <p>
 *     If a field of the original serialized struct is a string field TODO
 * </p>
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface StringField {

	IntegerField length() default @IntegerField(expectUniform = false, minValue = 0, maxValue = Integer.MAX_VALUE);
}
