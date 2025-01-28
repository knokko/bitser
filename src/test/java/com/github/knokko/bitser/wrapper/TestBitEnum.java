package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitEnum;
import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.io.BitCountStream;
import com.github.knokko.bitser.serialize.Bitser;
import org.junit.jupiter.api.Test;

import static com.github.knokko.bitser.wrapper.TestHelper.assertContains;
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

	@BitField
	private Season seasons;

	@BitField
	private Direction direction;

	@BitField(optional = true)
	private Element element;

	@Test
	public void test() {
		this.seasons = Season.WINTER;
		this.direction = Direction.UP;

		TestBitEnum loaded = new Bitser(true).deepCopy(this);
		assertEquals(Season.WINTER, loaded.seasons);
		assertEquals(Direction.UP, loaded.direction);
		assertNull(loaded.element);

		this.element = Element.WATER;
		loaded = new Bitser(false).deepCopy(this);
		assertEquals(Season.WINTER, loaded.seasons);
		assertEquals(Direction.UP, loaded.direction);
		assertEquals(Element.WATER, loaded.element);
	}

	@BitEnum(mode = BitEnum.Mode.Name)
	private static class NonEnumClass {}

	@BitStruct(backwardCompatible = false)
	private static class NonEnumStruct {

		@BitField
		@SuppressWarnings("unused")
		NonEnumClass nope;
	}

	@Test
	public void testNonEnumClass() {
		String errorMessage = assertThrows(InvalidBitFieldException.class,
				() -> new Bitser(false).serialize(new NonEnumStruct(), new BitCountStream())
		).getMessage();
		assertContains(errorMessage, "BitEnum can only be used on enums");
	}

	@SuppressWarnings("unused")
	@BitEnum(mode = BitEnum.Mode.Name)
	private enum MissingSeason {
		SUMMER,
		SPRING
	}

	@BitStruct(backwardCompatible = false)
	private static class SeasonStruct {

		@BitField
		@SuppressWarnings("unused")
		final Season season = Season.AUTUMN;
	}

	@BitStruct(backwardCompatible = false)
	private static class MissingSeasonStruct {

		@BitField
		@SuppressWarnings("unused")
		MissingSeason season;
	}

	@Test
	public void testDeletedEnumConstantName() {
		Bitser bitser = new Bitser(false);
		byte[] bytes = bitser.serializeToBytes(new SeasonStruct());

		String errorMessage = assertThrows(InvalidBitFieldException.class, () -> bitser.deserializeFromBytes(
				MissingSeasonStruct.class, bytes
		)).getMessage();
		assertContains(errorMessage, "Missing enum constant AUTUMN");
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

		@BitField
		@SuppressWarnings("unused")
		final Direction direction = Direction.DOWN;
	}

	@BitStruct(backwardCompatible = false)
	private static class MissingDirectionStruct {

		@BitField
		@SuppressWarnings("unused")
		MissingDirection direction;
	}

	@Test
	public void testDeletedEnumConstantOrdinal() {
		Bitser bitser = new Bitser(false);
		byte[] bytes = bitser.serializeToBytes(new DirectionStruct());

		String errorMessage = assertThrows(InvalidBitFieldException.class, () -> bitser.deserializeFromBytes(
				MissingDirectionStruct.class, bytes
		)).getMessage();
		assertContains(errorMessage, "Missing enum ordinal 3");
	}
}
