package com.github.knokko.bitser.test.wrapper;

import com.github.knokko.bitser.BitEnum;
import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.EnumField;
import com.github.knokko.bitser.io.BitCountStream;
import com.github.knokko.bitser.Bitser;
import org.junit.jupiter.api.Test;

import static com.github.knokko.bitser.test.wrapper.TestHelper.assertContains;
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
	@BitEnum(mode = BitEnum.Mode.Ordinal)
	private enum Direction {
		LEFT,
		RIGHT,
		UP,
		DOWN
	}

	@SuppressWarnings("unused")
	private enum Element {
		WATER,
		FIRE,
		AIR,
		EARTH
	}

	@SuppressWarnings("unused")
	private enum ReverseElement {
		EARTH,
		AIR,
		FIRE,
		WATER
	}

	@BitField
	private Season seasons;

	@BitField
	private Direction direction;

	@BitField(optional = true)
	@EnumField(mode = BitEnum.Mode.Ordinal)
	private Element element;

	@Test
	public void test() {
		this.seasons = Season.WINTER;
		this.direction = Direction.UP;

		TestBitEnum loaded = new Bitser(true).stupidDeepCopy(this);
		assertEquals(Season.WINTER, loaded.seasons);
		assertEquals(Direction.UP, loaded.direction);
		assertNull(loaded.element);

		this.element = Element.WATER;
		loaded = new Bitser(false).stupidDeepCopy(this);
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
		byte[] bytes = bitser.toBytes(new SeasonStruct());

		String errorMessage = assertThrows(InvalidBitFieldException.class, () -> bitser.fromBytes(
				MissingSeasonStruct.class, bytes
		)).getMessage();
		assertContains(errorMessage, "Missing enum constant AUTUMN");
		assertContains(errorMessage, "-> season");
	}

	@SuppressWarnings("unused")
	@BitEnum(mode = BitEnum.Mode.Ordinal)
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
		byte[] bytes = bitser.toBytes(new DirectionStruct());

		String errorMessage = assertThrows(InvalidBitFieldException.class, () -> bitser.fromBytes(
				MissingDirectionStruct.class, bytes
		)).getMessage();
		assertContains(errorMessage, "Missing enum ordinal 3");
		assertContains(errorMessage, "-> direction");
	}

	@BitStruct(backwardCompatible = false)
	private static class OverruleSeason {

		@BitField
		@EnumField(mode = BitEnum.Mode.Ordinal)
		Season season;
	}

	@Test
	public void testOverruleDefaultMode() {
		Bitser bitser = new Bitser(false);

		OverruleSeason overrule = new OverruleSeason();
		overrule.season = Season.SUMMER;

		DirectionStruct direction = bitser.fromBytes(
				DirectionStruct.class, bitser.toBytes(overrule)
		);
		assertEquals(Direction.LEFT, direction.direction);
	}

	@BitStruct(backwardCompatible = false)
	private static class Boss {

		@EnumField(mode = BitEnum.Mode.Ordinal)
		Element weakAgainst;

		@EnumField(mode = BitEnum.Mode.Name)
		Element strongAgainst;
	}

	@BitStruct(backwardCompatible = false)
	private static class MixedBoss {

		@EnumField(mode = BitEnum.Mode.Ordinal)
		ReverseElement weakAgainst;

		@EnumField(mode = BitEnum.Mode.Name)
		ReverseElement strongAgainst;
	}

	@Test
	public void testWithoutBitEnum() {
		Bitser bitser = new Bitser(true);

		Boss original = new Boss();
		original.weakAgainst = Element.FIRE;
		original.strongAgainst = Element.EARTH;

		MixedBoss mixed = bitser.fromBytes(MixedBoss.class, bitser.toBytes(original));
		assertEquals(ReverseElement.AIR, mixed.weakAgainst);
		assertEquals(ReverseElement.EARTH, mixed.strongAgainst);
	}
}
