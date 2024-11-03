package com.github.knokko.bitser.structs;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.FloatField;
import com.github.knokko.bitser.field.IntegerField;

@BitStruct(backwardCompatible = false)
public class WandCooldowns {

    @BitField(ordering = 1)
    @IntegerField(minValue = 1, expectUniform = true)
    public int cooldown;

    @BitField(ordering = 0)
    @IntegerField(minValue = 1, expectUniform = true)
    private int maxCharges = 3;

    @BitField(ordering = 2)
    @IntegerField(minValue = 0, expectUniform = true)
    public Integer rechargeTime;

//    @BitField(ordering = 3)
//    @FloatField(expectMultipleOf = 0.5)
//    public float refundProbability;
}
