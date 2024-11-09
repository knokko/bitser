package com.github.knokko.bitser.field;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface CollectionField {

	IntegerField size() default @IntegerField(expectUniform = false);

	boolean optionalValues() default false;

	String valueAnnotations();

	/**
	 * When true:
	 * <ul>
	 *     <li>The elements of the collection must be primitive (<b>byte</b>, <b>int</b>, etc...)</li>
	 *     <li>optionalValues must be false</li>
	 *     <li>valueAnnotations must be an empty string (so no variable-size encoding is possible)</li>
	 *     <li>
	 *         The contents will be converted to a <b>byte[]</b> before being written, which can improve the
	 *         encoding speed considerably (between 10 and 100 times on my machine).
	 *     </li>
	 * </ul>
	 */
	boolean writeAsBytes() default false;
}
