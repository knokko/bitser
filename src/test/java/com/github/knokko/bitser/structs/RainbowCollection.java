package com.github.knokko.bitser.structs;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.field.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

@BitStruct(backwardCompatible = false)
public class RainbowCollection {

	@SuppressWarnings("unused")
	@StringField(optional = false)
	private static final String STRINGS_VALUES = null;

	@SuppressWarnings("unused")
	@IntegerField(expectUniform = true, minValue = 510, maxValue = 520)
	private static final Integer INTS_VALUES = 0;

	@SuppressWarnings("unused")
	@IntegerField(expectUniform = true)
	private static final byte BYTES_VALUES = 0;

	@SuppressWarnings("unused")
	@IntegerField(expectUniform = true, minValue = 510, maxValue = 520)
	private static final Long LONGS_VALUES = 0L;

	@FloatField
	@SuppressWarnings("unused")
	private static final Double DOUBLE_VALUES = 0.0;

	@SuppressWarnings("unused")
	@IntegerField(expectUniform = false)
	private static final short SHORT_VALUES = 0;

	@SuppressWarnings("unused")
	@IntegerField(expectUniform = true)
	private static final int INT_ARRAY_VALUES = 0;

	@FloatField
	@SuppressWarnings("unused")
	private static final float FLOAT_VALUES = 0f;

	@FloatField
	@SuppressWarnings("unused")
	private static final double DOUBLE_ARRAY_VALUES = 0.0;

	@FloatField
	@SuppressWarnings("unused")
	private static final Float FLOAT_LIST_VALUES = 0f;

	@BitField(ordering = 0)
	@CollectionField(valueAnnotations = "STRINGS_VALUES")
	public String[] strings;

	@BitField(ordering = 1)
	@CollectionField(valueAnnotations = "INTS_VALUES")
	public ArrayList<Integer> ints;

	@BitField(ordering = 2)
	@CollectionField(valueAnnotations = "BYTES_VALUES")
	public byte[] bytes;

	@BitField(ordering = 3)
	@CollectionField(valueAnnotations = "LONGS_VALUES")
	public HashSet<Long> longs;

	@BitField(ordering = 4)
	@CollectionField(valueAnnotations = "DOUBLE_VALUES")
	public LinkedList<Double> doubles;

	@BitField(ordering = 5)
	@CollectionField(valueAnnotations = "SHORT_VALUES")
	public short[] shorts;

	@BitField(ordering = 6)
	@CollectionField(valueAnnotations = "INT_ARRAY_VALUES")
	public int[] intArray;

	@BitField(ordering = 7)
	@CollectionField(valueAnnotations = "FLOAT_VALUES")
	public float[] floats;

	@BitField(ordering = 8)
	@CollectionField(valueAnnotations = "DOUBLE_ARRAY_VALUES")
	public double[] doubleArray;

	@BitField(ordering = 9)
	@CollectionField(valueAnnotations = "FLOAT_LIST_VALUES")
	public ArrayList<Float> floatList;
}
