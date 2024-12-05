package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.io.BitCountStream;
import com.github.knokko.bitser.io.BitserHelper;
import com.github.knokko.bitser.serialize.Bitser;
import org.junit.jupiter.api.Test;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

public class TestBitStruct {

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

		WandCooldowns loadedCooldowns = BitserHelper.serializeAndDeserialize(new Bitser(true), wandCooldowns);

		assertEquals(5, loadedCooldowns.cooldown);
		assertEquals(4, loadedCooldowns.maxCharges);
		assertEquals(20, loadedCooldowns.rechargeTime);
	}

	@BitStruct(backwardCompatible = false)
	private static class Chain {

		@BitField(ordering = 0, optional = true)
		public Chain next;

		@BitField(ordering = 1)
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

		Chain loaded = BitserHelper.serializeAndDeserialize(new Bitser(true), root);
		assertEquals(10, loaded.properties.strength);
		assertEquals(2, loaded.next.properties.strength);
		assertNull(loaded.next.next);
	}

	@BitStruct(backwardCompatible = false)
	private static class NoAnnotations {

		@BitField(ordering = 0)
		@SuppressWarnings("unused")
		int nope;
	}

	@Test
	public void testNoAnnotationsError() {
		InvalidBitFieldException invalid = assertThrows(InvalidBitFieldException.class,
				() -> new Bitser(false).serialize(new NoAnnotations(), new BitCountStream())
		);
		assertTrue(
				invalid.getMessage().contains("Missing annotations for"),
				"Expected " + invalid.getMessage() + " to contain \"Missing annotations for\""
		);
	}

	private static class Entity {

		@BitField(ordering = 1)
		@IntegerField(expectUniform = false)
		int x;

		@SuppressWarnings("unused")
		long tempStuff;

		@BitField(ordering = 0)
		@IntegerField(expectUniform = true)
		int y;
	}

	private static class Rectangle extends Entity {

		@SuppressWarnings("unused")
		boolean temporary;

		@BitField(ordering = 0)
		@IntegerField(expectUniform = false, minValue = 1)
		int width;

		@BitField(ordering = 1)
		@IntegerField(expectUniform = true, minValue = 1)
		int height;
	}

	@BitStruct(backwardCompatible = false)
	private static class ColoredRectangle extends Rectangle {

		@BitField(ordering = 0)
		String color;
	}

	@Test
	public void testInheritance() throws IOException {
		ColoredRectangle rectangle = new ColoredRectangle();
		rectangle.x = -12;
		rectangle.y = -34;
		rectangle.width = 56;
		rectangle.height = 789;
		rectangle.color = "red";

		ColoredRectangle loaded = BitserHelper.serializeAndDeserialize(new Bitser(false), rectangle);
		assertEquals(-12, loaded.x);
		assertEquals(-34, loaded.y);
		assertEquals(56, loaded.width);
		assertEquals(789, loaded.height);
		assertEquals("red", loaded.color);
	}
}
