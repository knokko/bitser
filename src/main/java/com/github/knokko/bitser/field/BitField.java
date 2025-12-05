package com.github.knokko.bitser.field;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 *     Use this annotation to mark a field of a {@link com.github.knokko.bitser.BitStruct} as a <i>BitField</i>,
 *     which causes bitser to (de)serialize it.
 * </p>
 *
 * <p>
 *     This annotation can also be used to mark fields as {@link #optional()} (nullable), or to define their ID.
 *     The latter is required for all <i>BitFields</i> of <b>backward-compatible</b> structs.
 * </p>
 *
 * <p>
 *     Note that this annotation is optional for some fields of non-backward-compatible structs: when a field has
 *     another bitser annotation (e.g. {@link IntegerField}), but no <i>BitField</i>, bitser will treat it like it has
 *     {@code @BitField(optional = false)}.
 * </p>
 *
 * <p>
 *     Fields without any bitser annotations will be ignored by bitser.
 * </p>
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface BitField {

	/**
	 * The ID of the field, which is needed for backward-compatible structs.
	 * When serializing a field with {@link com.github.knokko.bitser.Bitser#BACKWARD_COMPATIBLE},
	 * the ID of each serialized field is stored alongside the serialized value. Upon deserializing, each serialized
	 * value is assigned to the field with the corresponding serialized ID.
	 */
	int id() default -1;

	/**
	 * Whether this field is allowed to be {@code null}
	 */
	boolean optional() default false;
}
