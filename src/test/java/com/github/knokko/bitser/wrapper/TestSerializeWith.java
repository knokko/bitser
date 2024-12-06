package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.field.*;
import com.github.knokko.bitser.io.BitCountStream;
import com.github.knokko.bitser.io.BitserHelper;
import com.github.knokko.bitser.serialize.Bitser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class TestSerializeWith {

	@BitStruct(backwardCompatible = false)
	static class StableStruct {

		@SuppressWarnings("unused")
		@BitField(ordering = 0)
		@StableReferenceFieldId
		final UUID id = UUID.randomUUID();

		@BitField(ordering = 1)
		@IntegerField(expectUniform = false)
		final int test;

		@SuppressWarnings("unused")
		StableStruct() {
			this(0);
		}

		StableStruct(int test) {
			this.test = test;
		}
	}

	@BitStruct(backwardCompatible = false)
	static class TargetStruct {

		@BitField(ordering = 0)
		@ReferenceFieldTarget(label = "stable")
		final ArrayList<StableStruct> stable = new ArrayList<>();

		@BitField(ordering = 1)
		@ReferenceFieldTarget(label = "unstable")
		final ArrayList<String> unstable = new ArrayList<>();
	}

	@BitStruct(backwardCompatible = false)
	static class ReferenceStruct {

		@BitField(ordering = 0)
		@ReferenceField(stable = true, label = "stable")
		@NestedFieldSetting(path = "", sizeField = @IntegerField(expectUniform = true, minValue = 0, maxValue = 3))
		final ArrayList<StableStruct> stable = new ArrayList<>();

		@BitField(ordering = 1)
		@ReferenceField(stable = false, label = "unstable")
		@NestedFieldSetting(path = "", sizeField = @IntegerField(expectUniform = true, minValue = 0, maxValue = 3))
		final ArrayList<String> unstable = new ArrayList<>();
	}

	@Test
	public void testSingleStable() throws IOException {
		TargetStruct targets = new TargetStruct();
		targets.stable.add(new StableStruct(12));

		ReferenceStruct references = new ReferenceStruct();
		references.stable.add(targets.stable.get(0));

		ReferenceStruct loaded = BitserHelper.serializeAndDeserialize(new Bitser(false), references, targets);
		assertEquals(1, loaded.stable.size());
		assertEquals(0, loaded.unstable.size());
		assertSame(targets.stable.get(0), loaded.stable.get(0));

		BitCountStream counter = new BitCountStream();
		new Bitser(true).serialize(references, counter, targets);

		// 2 bits for unstable counts per label
		// 2 bits for stable size
		// 128 bits for stable id
		// 2 bits for unstable size
		assertEquals(134, counter.getCounter());
	}

	@Test
	public void testSingleUnstable() throws IOException {
		TargetStruct targets = new TargetStruct();
		targets.unstable.add("hello");

		ReferenceStruct references = new ReferenceStruct();
		references.unstable.add(targets.unstable.get(0));

		ReferenceStruct loaded = BitserHelper.serializeAndDeserialize(new Bitser(false), references, targets);
		assertEquals(0, loaded.stable.size());
		assertEquals(1, loaded.unstable.size());
		assertSame(targets.unstable.get(0), loaded.unstable.get(0));

		BitCountStream counter = new BitCountStream();
		new Bitser(true).serialize(references, counter, targets);

		// 2 bits for unstable counts per label
		// 2 bits for stable size
		// 2 bits for unstable size
		// 0 bits for unstable id
		assertEquals(6, counter.getCounter());
	}

	@BitStruct(backwardCompatible = false)
	static class Web {

		@BitField(ordering = 0)
		final TargetStruct ownTargets = new TargetStruct();

		@BitField(ordering = 1)
		final ReferenceStruct references = new ReferenceStruct();
	}

	@Test
	public void testComplexWeb() throws IOException {
		Web web = new Web();
		web.ownTargets.stable.add(new StableStruct(50));
		web.ownTargets.unstable.add("internal");
		web.references.stable.add(web.ownTargets.stable.get(0));
		web.references.unstable.add(web.ownTargets.unstable.get(0));

		TargetStruct with1 = new TargetStruct();
		with1.stable.add(new StableStruct(67));
		with1.unstable.add("external1");

		TargetStruct with2 = new TargetStruct();
		with2.stable.add(new StableStruct(89));
		with2.unstable.add("external2");

		web.references.stable.add(with1.stable.get(0));
		web.references.unstable.add(with1.unstable.get(0));

		web.references.stable.add(with2.stable.get(0));
		web.references.unstable.add(with2.unstable.get(0));

		Web loaded = BitserHelper.serializeAndDeserialize(new Bitser(true), web, with2, with1);
		assertEquals(1, loaded.ownTargets.stable.size());
		assertEquals(1, loaded.ownTargets.unstable.size());
		assertEquals(3, loaded.references.stable.size());
		assertEquals(3, loaded.references.unstable.size());
		assertSame(loaded.ownTargets.stable.get(0), loaded.references.stable.get(0));
		assertSame(loaded.ownTargets.unstable.get(0), loaded.references.unstable.get(0));
		assertSame(with1.stable.get(0), loaded.references.stable.get(1));
		assertSame(with1.unstable.get(0), loaded.references.unstable.get(1));
		assertSame(with2.stable.get(0), loaded.references.stable.get(2));
		assertSame(with2.unstable.get(0), loaded.references.unstable.get(2));
	}
}
