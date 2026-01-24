package com.github.knokko.bitser.test;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.Bitser;
import com.github.knokko.bitser.SimpleLazyBits;
import com.github.knokko.bitser.field.*;
import com.github.knokko.bitser.options.WithParameter;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestCollectInstances {

	interface DummyInterface { }

	@BitStruct(backwardCompatible = false)
	static class MiniStruct implements DummyInterface {

		@IntegerField(expectUniform = false)
		@SuppressWarnings("unused")
		int x;
	}

	@BitStruct(backwardCompatible = false)
	static class MiniStruct2 extends MiniStruct {

		@FloatField
		@SuppressWarnings("unused")
		float f = 3f;

		@FloatField
		@BitField(id = 2)
		@SuppressWarnings("unused")
		float product(FunctionContext context) {
			return f * (Float) context.withParameters.get("factor");
		}
	}

	@BitStruct(backwardCompatible = false)
	static class ParentStruct {

		@BitField
		MiniStruct mini;

		@ReferenceFieldTarget(label = "mini")
		MiniStruct[] children;

		@ReferenceField(stable = false, label = "mini")
		MiniStruct favoriteChild;
	}

	@BitStruct(backwardCompatible = false)
	static class RootStruct {

		@BitField
		SimpleLazyBits<MiniStruct> lazy;

		@BitField
		final HashMap<String, MiniStruct2> mapping = new HashMap<>();

		@BitField
		ParentStruct parent;
	}

	@Test
	public void runTest() {
		var bitser = new Bitser(true);
		var root = new RootStruct();

		root.lazy = new SimpleLazyBits<>(new MiniStruct());
		root.mapping.put("test", new MiniStruct2());
		root.parent = new ParentStruct();
		root.parent.mini = new MiniStruct();
		root.parent.children = new MiniStruct[] { new MiniStruct(), new MiniStruct() };
		root.parent.favoriteChild = root.parent.children[1];

		var collected = new HashMap<Class<?>, Collection<Object>>();
		collected.put(MiniStruct.class, new HashSet<>());
		collected.put(DummyInterface.class, new ArrayList<>());
		collected.put(RootStruct.class, new ArrayList<>());
		collected.put(Float.class, new ArrayList<>());
		bitser.collectInstances(root, collected, new WithParameter("factor", 7f));
		assertEquals(4, collected.size());

		var expectedMini = new HashSet<>();
		expectedMini.add(root.lazy.get());
		expectedMini.add(root.mapping.get("test"));
		expectedMini.add(root.parent.mini);
		Collections.addAll(expectedMini, root.parent.children);
		assertEquals(expectedMini, collected.get(MiniStruct.class));

		var actualDummy = collected.get(DummyInterface.class);
		assertEquals(expectedMini.size(), actualDummy.size());
		assertEquals(expectedMini, new HashSet<>(actualDummy));

		var expectedRoot = new ArrayList<>();
		expectedRoot.add(root);
		assertEquals(expectedRoot, collected.get(RootStruct.class));

		var expectedFloats = new HashSet<>();
		expectedFloats.add(3f);
		expectedFloats.add(21f);

		var actualFloats = collected.get(Float.class);
		assertEquals(expectedFloats.size(), actualFloats.size());
		assertEquals(expectedFloats, new HashSet<>(actualFloats));
	}
}
