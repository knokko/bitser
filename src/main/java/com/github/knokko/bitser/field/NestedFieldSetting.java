package com.github.knokko.bitser.field;

import java.lang.annotation.*;

@Repeatable(NestedFieldSettings.class)
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface NestedFieldSetting {

    String path();

    String fieldName() default "";

    boolean optional() default false;

    /**
     * When true:
     * <ul>
     *     <li>
     *         The collection must be a primitive array (e.g. <b>byte[]</b> or <b>int[]</b>).
     *     </li>
     *     <li>
     *         The contents will be converted to a <b>byte[]</b> (if needed) before being written, which can improve the
     *         encoding speed considerably (between 10 and 100 times on my machine).
     *     </li>
     * </ul>
     */
    boolean writeAsBytes() default false;

    IntegerField sizeField() default @IntegerField(expectUniform = false, minValue = 0, maxValue = Integer.MAX_VALUE);
}
