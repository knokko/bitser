package com.github.knokko.bitser.field;

import java.lang.annotation.*;

@Repeatable(NestedFieldSettings.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NestedFieldSetting {

    String path();

    String fieldName() default "";

    boolean optional() default false;

    /**
     * When true:
     * <ul>
     *     <li>
     *         The elements of the collection must either be primitive (<b>byte</b>, <b>int</b>, etc...), or
     *         represent a primitive type (e.g. <b>Byte</b>).
     *     </li>
     *     <li>The elements of the collection must <b>not</b> be optional.</li>
     *     <li>
     *         The contents will be converted to a <b>byte[]</b> before being written, which can improve the
     *         encoding speed considerably (between 10 and 100 times on my machine).
     *     </li>
     * </ul>
     */
    boolean writeAsBytes() default false;

    IntegerField sizeField() default @IntegerField(expectUniform = false, minValue = 0, maxValue = Integer.MAX_VALUE);
}
