package com.github.knokko.bitser.backward;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.ClassField;
import com.github.knokko.bitser.field.FloatField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.serialize.Bitser;
import org.junit.jupiter.api.Test;

import static com.github.knokko.bitser.wrapper.TestHelper.assertContains;
import static org.junit.jupiter.api.Assertions.*;

public class TestInheritanceBackwardCompatibility {

	private static class OldAnimal {

		@SuppressWarnings("unused")
		private static final Class<?>[] BITSER_HIERARCHY = { OldBird.class, OldFish.class };

		@BitField(id = 0)
		@IntegerField(expectUniform = false, minValue = 0)
		int numLegs;
	}

	@BitStruct(backwardCompatible = true)
	private static class NewAnimal {

		@SuppressWarnings("unused")
		private static final Class<?>[] BITSER_HIERARCHY = {
				NewBird.class, NewFish.class, NewAnimal.class, NewReptile.class, NewSnake.class
		};

		@BitField(id = 0)
		@IntegerField(expectUniform = false, minValue = 0)
		int numLegs;

		@BitField(id = 1)
		boolean isHungry;
	}

	@BitStruct(backwardCompatible = true)
	private static class OldBird extends OldAnimal {

		@BitField(id = 0)
		@FloatField(expectMultipleOf = 0.1)
		float flySpeed;
	}

	@BitStruct(backwardCompatible = true)
	private static class NewBird extends NewAnimal {

		@BitField(id = 0, optional = true)
		@FloatField(expectMultipleOf = 0.5)
		Float flySpeed;
	}

	@BitStruct(backwardCompatible = true)
	private static class OldFish extends OldAnimal {

		@BitField(id = 5)
		boolean canJump;

		@BitField(id = 3)
		@FloatField(expectMultipleOf = 0.5)
		double swimSpeed;
	}

	@BitStruct(backwardCompatible = true)
	private static class NewFish extends NewAnimal {

		@BitField(id = 5)
		boolean canJump;

		@BitField(id = 3)
		@FloatField(expectMultipleOf = 0.5)
		float swimSpeed;
	}

	@BitStruct(backwardCompatible = true)
	private static class NewSnake extends NewAnimal {

		@SuppressWarnings("unused")
		@BitField(id = 0)
		boolean isVenomous;
	}

	@BitStruct(backwardCompatible = true)
	private static class NewReptile extends NewAnimal {

		@SuppressWarnings("unused")
		@BitField(id = 0)
		boolean isMortal;
	}

	@BitStruct(backwardCompatible = true)
	private static class OldZoo {

		@BitField(id = 0)
		@ClassField(root = OldAnimal.class)
		OldAnimal[] animals;
	}

	@BitStruct(backwardCompatible = true)
	private static class NewZoo {

		@BitField(id = 0)
		@ClassField(root = NewAnimal.class)
		NewAnimal[] animals;
	}

	@Test
	public void testSimpleInheritanceBackwardCompatibility() {
		Bitser bitser = new Bitser(false);
		OldFish fish1 = new OldFish();
		fish1.canJump = false;
		fish1.swimSpeed = 3.5;

		OldFish fish2 = new OldFish();
		fish2.canJump = true;
		fish2.swimSpeed = 0.75;

		OldBird bird = new OldBird();
		bird.flySpeed = 20;
		bird.numLegs = 2;

		OldZoo oldZoo = new OldZoo();
		oldZoo.animals = new OldAnimal[] { fish1, bird, fish2 };

		NewZoo newZoo = bitser.deserializeFromBytes(NewZoo.class, bitser.serializeToBytes(
				oldZoo, Bitser.BACKWARD_COMPATIBLE
		), Bitser.BACKWARD_COMPATIBLE);
		assertEquals(3, newZoo.animals.length);

		NewFish newFish1 = (NewFish) newZoo.animals[0];
		assertFalse(newFish1.canJump);
		assertEquals(3.5, newFish1.swimSpeed);
		assertEquals(0, newFish1.numLegs);

		NewBird newBird = (NewBird) newZoo.animals[1];
		assertEquals(20, newBird.flySpeed);
		assertEquals(2, newBird.numLegs);
		assertFalse(newBird.isHungry);

		NewFish newFish2 = (NewFish) newZoo.animals[2];
		assertTrue(newFish2.canJump);
		assertEquals(0.75f, newFish2.swimSpeed);

		OldZoo back = bitser.deserializeFromBytes(OldZoo.class, bitser.serializeToBytes(
				newZoo, Bitser.BACKWARD_COMPATIBLE
		), Bitser.BACKWARD_COMPATIBLE);
		assertEquals(3, back.animals.length);

		OldFish backFish1 = (OldFish) back.animals[0];
		assertFalse(backFish1.canJump);
		assertEquals(3.5, backFish1.swimSpeed);
		assertEquals(0, backFish1.numLegs);

		OldBird backBird = (OldBird) back.animals[1];
		assertEquals(20, backBird.flySpeed);
		assertEquals(2, backBird.numLegs);

		OldFish backFish2 = (OldFish) back.animals[2];
		assertTrue(backFish2.canJump);
		assertEquals(0, backFish2.numLegs);
		assertEquals(0.75, backFish2.swimSpeed);

		newZoo.animals[0] = new NewReptile();
		String errorMessage = assertThrows(InvalidBitValueException.class, () -> bitser.deserializeFromBytes(
				OldZoo.class, bitser.serializeToBytes(newZoo, Bitser.BACKWARD_COMPATIBLE), Bitser.BACKWARD_COMPATIBLE
		)).getMessage();
		assertContains(errorMessage, "unknown subclass");
		assertContains(errorMessage, "OldAnimal[]");
		assertContains(errorMessage, "OldZoo.animals");
	}
}
