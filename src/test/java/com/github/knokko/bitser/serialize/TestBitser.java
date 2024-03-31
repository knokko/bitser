package com.github.knokko.bitser.serialize;

import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.structs.Chain;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
}
