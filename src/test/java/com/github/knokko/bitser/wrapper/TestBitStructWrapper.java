package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.field.StructField;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.BitserCache;
import org.junit.jupiter.api.Test;

import java.io.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TestBitStructWrapper {

	@BitStruct(backwardCompatible = false)
	private static class WandCooldowns {

		@BitField(ordering = 1)
		@IntegerField(minValue = 1, expectUniform = true)
		public int cooldown;

		@BitField(ordering = 0)
		@IntegerField(minValue = 1, expectUniform = true)
		private int maxCharges = 3;

		@BitField(ordering = 2)
		@IntegerField(minValue = 0, expectUniform = true)
		public Integer rechargeTime;
	}

	@Test
	public void testWandCooldowns() throws IOException {
		WandCooldowns wandCooldowns = new WandCooldowns();
		wandCooldowns.cooldown = 5;
		wandCooldowns.maxCharges = 4;
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
		assertEquals(4, loadedCooldowns.maxCharges);
		assertEquals(20, loadedCooldowns.rechargeTime);
	}

	@BitStruct(backwardCompatible = false)
	private static class Chain {

		@BitField(ordering = 0, optional = true)
		@StructField
		public Chain next;

		@BitField(ordering = 1)
		@StructField
		public Properties properties;

		@BitStruct(backwardCompatible = false)
		public static class Properties {

			@BitField(ordering = 0)
			@IntegerField(expectUniform = false)
			public int strength;

			@SuppressWarnings("unused")
			private Properties() {
				this.strength = 0;
			}

			public Properties(int strength) {
				this.strength = strength;
			}
		}
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
