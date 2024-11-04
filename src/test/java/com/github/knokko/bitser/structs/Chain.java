package com.github.knokko.bitser.structs;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.field.StructField;

@BitStruct(backwardCompatible = false)
public class Chain {

    @BitField(ordering = 0, optional = true)
    @StructField
    public Chain next;

    @BitField(ordering = 1)
    @StructField
    public Properties properties;

    @BitStruct(backwardCompatible = false)
    public static class Properties {

        public Properties() {
            this.strength = 0;
        }

        public Properties(int strength) {
            this.strength = strength;
        }

        @BitField(ordering = 0)
        @IntegerField(expectUniform = false)
        public int strength;
    }
}
