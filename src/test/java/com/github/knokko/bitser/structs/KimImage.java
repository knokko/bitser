package com.github.knokko.bitser.structs;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.CollectionField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.field.StringField;

import java.util.UUID;

@BitStruct(backwardCompatible = false)
public class KimImage {

	@SuppressWarnings("unused")
	@IntegerField(expectUniform = true)
	private static final int COMPRESSED_DATA_ELEMENTS = 0;

	@BitField(ordering = 0)
	public final UUID id = UUID.randomUUID();

	@BitField(ordering = 2)
	@StringField(optional = true)
	public String name;

	@BitField(ordering = 1)
	@CollectionField(size = @IntegerField(expectUniform = false), valueAnnotations = "COMPRESSED_DATA_ELEMENTS")
	public int[] compressedData;
}
