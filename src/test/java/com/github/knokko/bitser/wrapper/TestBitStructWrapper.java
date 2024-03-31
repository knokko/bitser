package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.structs.Chain;
import com.github.knokko.bitser.structs.WandCooldowns;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.BitserCache;
import org.junit.jupiter.api.Test;

import java.io.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TestBitStructWrapper {

    @Test
    public void testWandCooldowns() throws IOException {
        WandCooldowns wandCooldowns = new WandCooldowns();
        wandCooldowns.cooldown = 5;
        wandCooldowns.rechargeTime = 20;

        BitserWrapper<WandCooldowns> wandCooldownsWrapper = BitserWrapper.wrap(WandCooldowns.class);
        BitserCache cache = new BitserCache(false);

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        BitOutputStream bitOutput = new BitOutputStream(byteStream);
        wandCooldownsWrapper.write(wandCooldowns, bitOutput, cache);
        bitOutput.finish();

        BitInputStream bitInput = new BitInputStream(new ByteArrayInputStream(byteStream.toByteArray()));
        WandCooldowns loadedCooldowns = wandCooldownsWrapper.read(bitInput, cache);
        bitInput.close();

        assertEquals(5, loadedCooldowns.cooldown);
        assertEquals(20, loadedCooldowns.rechargeTime);
    }

    @Test
    public void testChain() throws IOException {
        Chain root = new Chain();
        root.properties = new Chain.Properties(10);
        root.next = new Chain();
        root.next.properties = new Chain.Properties(2);

        BitserWrapper<Chain> wrapper = BitserWrapper.wrap(Chain.class);
        BitserCache cache = new BitserCache(false);

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        BitOutputStream bitOutput = new BitOutputStream(byteStream);
        wrapper.write(root, bitOutput, cache);
        bitOutput.finish();

        Chain loaded = wrapper.read(new BitInputStream(new ByteArrayInputStream(byteStream.toByteArray())), cache);
        assertEquals(10, loaded.properties.strength);
        assertEquals(2, loaded.next.properties.strength);
        assertNull(loaded.next.next);
    }
}
