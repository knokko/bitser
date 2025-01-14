package com.github.knokko.bitser.connection;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.field.BitField;

import java.util.ArrayList;

@BitStruct(backwardCompatible = false)
public class BitPacket {

	@BitField(ordering = 0)
	public final ArrayList<BitStructChange> changes = new ArrayList<>();
}
