package com.github.knokko.bitser.serialize;

import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.structs.Chain;
import com.github.knokko.bitser.structs.KimImage;
import com.github.knokko.bitser.structs.RainbowCollection;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;

import static org.junit.jupiter.api.Assertions.*;

public class TestBitser {

    @Test
    public void testChain() throws IOException {
        Bitser bitser = new Bitser(true);

        Chain chain = new Chain();
        chain.properties = new Chain.Properties(3);
        chain.next = new Chain();
        chain.next.properties = new Chain.Properties();

        ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
        BitOutputStream bitOutput = new BitOutputStream(byteOutput);
        bitser.serialize(chain, bitOutput);
        bitOutput.finish();

        Chain loaded = bitser.deserialize(Chain.class, new BitInputStream(new ByteArrayInputStream(byteOutput.toByteArray())));
        assertEquals(3, loaded.properties.strength);
        assertEquals(0, loaded.next.properties.strength);
        assertNull(loaded.next.next);
    }

    @Test
    public void testKimImage() throws IOException {
        Bitser bitser = new Bitser(true);

        KimImage image = new KimImage();
        image.compressedData = new int[] { 12, 345, 6789, 12345, 6789012 };
        image.name = "hello";

        ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
        BitOutputStream bitOutput = new BitOutputStream(byteOutput);
        bitser.serialize(image, bitOutput);
        bitOutput.finish();

        KimImage loaded = bitser.deserialize(KimImage.class, new BitInputStream(new ByteArrayInputStream(byteOutput.toByteArray())));
        assertEquals(image.id, loaded.id);
        assertArrayEquals(image.compressedData, loaded.compressedData);
        assertEquals("hello", loaded.name);
    }

    @Test
    public void testRainbowCollection() throws IOException {
        RainbowCollection rainbow = new RainbowCollection();
        rainbow.strings = new String[] { "hello", "", "world!" };
        rainbow.ints = new ArrayList<>(3);
        rainbow.ints.add(512);
        rainbow.ints.add(518);
        rainbow.ints.add(510);
        rainbow.bytes = new byte[] { -100, 46, 120, -8 };
        rainbow.longs = new HashSet<>(2);
        rainbow.longs.add(520L);
        rainbow.longs.add(515L);
        rainbow.doubles = new LinkedList<>();
        rainbow.doubles.add(-123.45);
        rainbow.doubles.add(67.89);
        rainbow.shorts = new short[] { -10_000, 12345, 32109 };
        rainbow.intArray = new int[] { 12345, 987, -37384 };
        rainbow.floats = new float[] { 123f, -9274f, Float.MIN_VALUE, Float.NaN, Float.POSITIVE_INFINITY };
        rainbow.doubleArray = new double[] { 1234559.832, Double.MAX_VALUE };
        rainbow.floatList = new ArrayList<>(1);
        rainbow.floatList.add(-12345f);

        Bitser bitser = new Bitser(true);
        ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
        BitOutputStream bitOutput = new BitOutputStream(byteOutput);
        bitser.serialize(rainbow, bitOutput);
        bitOutput.finish();

        RainbowCollection loaded = bitser.deserialize(RainbowCollection.class, new BitInputStream(new ByteArrayInputStream(byteOutput.toByteArray())));
        assertArrayEquals(new String[] { "hello", "", "world!" }, loaded.strings);
        assertEquals(rainbow.ints, loaded.ints);
        assertArrayEquals(rainbow.bytes, loaded.bytes);
        assertEquals(rainbow.longs, loaded.longs);
        assertEquals(rainbow.doubles, loaded.doubles);
        assertArrayEquals(rainbow.shorts, loaded.shorts);
        assertArrayEquals(rainbow.intArray, loaded.intArray);
        assertArrayEquals(rainbow.floats, loaded.floats);
        assertArrayEquals(rainbow.doubleArray, loaded.doubleArray);
        assertEquals(rainbow.floatList, loaded.floatList);
    }
}
