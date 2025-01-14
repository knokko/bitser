package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitEnum;
import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.io.BitCountStream;
import com.github.knokko.bitser.io.BitserHelper;
import com.github.knokko.bitser.serialize.Bitser;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@BitStruct(backwardCompatible = false)
public class TestBitEnum {

	@SuppressWarnings("unused")
	@BitEnum(mode = BitEnum.Mode.Name)
	private enum Season {
		SUMMER,
		AUTUMN,
		WINTER,
		SPRING
	}

	@SuppressWarnings("unused")
	@BitEnum(mode = BitEnum.Mode.UniformOrdinal)
	private enum Direction {
		LEFT,
		RIGHT,
		UP,
		DOWN
	}

	@SuppressWarnings("unused")
	@BitEnum(mode = BitEnum.Mode.VariableIntOrdinal)
	private enum Element {
		WATER,
		FIRE,
		AIR,
		EARTH
	}

	@BitField(ordering = 0)
	private Season seasons;

	@BitField(ordering = 2)
	private Direction direction;

	@BitField(ordering = 1, optional = true)
	private Element element;

	@Test
	public void test() throws IOException {
		this.seasons = Season.WINTER;
		this.direction = Direction.UP;

		TestBitEnum loaded = BitserHelper.serializeAndDeserialize(new Bitser(true), this);
		assertEquals(Season.WINTER, loaded.seasons);
		assertEquals(Direction.UP, loaded.direction);
		assertNull(loaded.element);

		this.element = Element.WATER;
		loaded = BitserHelper.serializeAndDeserialize(new Bitser(true), this);
		assertEquals(Season.WINTER, loaded.seasons);
		assertEquals(Direction.UP, loaded.direction);
		assertEquals(Element.WATER, loaded.element);
	}

	@BitEnum(mode = BitEnum.Mode.Name)
	private static class NonEnumClass {}

	@BitStruct(backwardCompatible = false)
	private static class NonEnumStruct {

		@BitField(ordering = 0)
		@SuppressWarnings("unused")
		NonEnumClass nope;
	}

	@Test
	public void testNonEnumClass() {
		InvalidBitFieldException invalid = assertThrows(InvalidBitFieldException.class,
				() -> new Bitser(false).serialize(new NonEnumStruct(), new BitCountStream())
		);
		assertTrue(
				invalid.getMessage().contains("BitEnum can only be used on enums"),
				"Expected " + invalid.getMessage() + " to contain \"BitEnum can only be used on enums\""
		);
	}

	@SuppressWarnings("unused")
	@BitEnum(mode = BitEnum.Mode.Name)
	private enum MissingSeason {
		SUMMER,
		SPRING
	}

	@BitStruct(backwardCompatible = false)
	private static class SeasonStruct {

		@BitField(ordering = 0)
		@SuppressWarnings("unused")
		final Season season = Season.AUTUMN;
	}

	@BitStruct(backwardCompatible = false)
	private static class MissingSeasonStruct {

		@BitField(ordering = 0)
		@SuppressWarnings("unused")
		MissingSeason season;
	}

	@Test
	public void testDeletedEnumConstantName() {
		Bitser bitser = new Bitser(false);
		byte[] bytes = bitser.serializeToBytes(new SeasonStruct());

		InvalidBitFieldException invalid = assertThrows(InvalidBitFieldException.class, () -> bitser.deserializeFromBytes(
				MissingSeasonStruct.class, bytes
		));
		assertTrue(
				invalid.getMessage().contains("Missing enum constant AUTUMN"),
				"Expected " + invalid.getMessage() + " to contain \"Missing enum constant AUTUMN\""
		);
	}

	@SuppressWarnings("unused")
	@BitEnum(mode = BitEnum.Mode.UniformOrdinal)
	private enum MissingDirection {
		LEFT,
		RIGHT,
		UP
	}

	@BitStruct(backwardCompatible = false)
	private static class DirectionStruct {

		@BitField(ordering = 0)
		@SuppressWarnings("unused")
		final Direction direction = Direction.DOWN;
	}

	@BitStruct(backwardCompatible = false)
	private static class MissingDirectionStruct {

		@BitField(ordering = 0)
		@SuppressWarnings("unused")
		MissingDirection direction;
	}

	@Test
	public void testDeletedEnumConstantOrdinal() {
		Bitser bitser = new Bitser(false);
		byte[] bytes = bitser.serializeToBytes(new DirectionStruct());

		InvalidBitFieldException invalid = assertThrows(InvalidBitFieldException.class, () -> bitser.deserializeFromBytes(
				MissingDirectionStruct.class, bytes
		));
		assertTrue(
				invalid.getMessage().contains("Missing enum ordinal 3"),
				"Expected " + invalid.getMessage() + " to contain \"Missing enum ordinal 3\""
		);
	}
}
