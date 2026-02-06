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

	/**
	 * <p>
	 *     Whether this field should read its value from a <b>method</b> that is annotated with
	 *     {@code @BitField(id = sameId)}.
	 * </p>
	 *
	 * <p>
	 *     This is {@code false} by default, and should almost always be {@code false}.
	 *     Choosing {@code true} is only allowed if:
	 * </p>
	 *
	 * <ul>
	 *     <li>This {@code @BitField} annotation is put on a <b>field</b>, and</li>
	 *     <li>the {@link #id()} is non-negative, and</li>
	 *     <li>
	 *         the struct containing this field has a <b>method</b> that is annotated with
	 *         {@code @BitField(id = sameIdAsThisField)}.
	 *     </li>
	 * </ul>
	 *
	 * <p>
	 *     When this property is {@code true}, this field will never be serialized. When this field is
	 *     <b>de</b>serialized, it will take the result of the method that is annotated with
	 *     {@code @BitField(id = sameIdAsThisField)}.
	 * </p>
	 *
	 * <p>
	 *     You can use this to e.g. derive the value of a field from other fields, and it allows you to control the
	 *     serialized values using <i>with</i> parameters.
	 * </p>
	 */
	boolean readsMethodResult() default false;
}
