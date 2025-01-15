package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.connection.BitListConnection;
import com.github.knokko.bitser.connection.BitStructConnection;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.field.NestedFieldSetting;
import com.github.knokko.bitser.serialize.Bitser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class TestBitListConnection {

	@BitStruct(backwardCompatible = false)
	private static class Root {

		@SuppressWarnings("unused")
		@BitField(ordering = 0)
		String hello = "world";

		@BitField(ordering = 1)
		@IntegerField(expectUniform = false)
		@NestedFieldSetting(path = "c", optional = true)
		ArrayList<Integer> integers = new ArrayList<>();

		@BitField(ordering = 2)
		ArrayList<String> strings = new ArrayList<>();
	}

	@Test
	public void testIntegerList() throws IOException {
		Bitser bitser = new Bitser(false);
		TestBitStructConnection.ChangeTracker tracker = new TestBitStructConnection.ChangeTracker(1);

		Root original = new Root();
		original.integers.add(3);
		original.integers.add(null);
		original.strings.add("ok");

		BitStructConnection<Root> root1 = bitser.createStructConnection(bitser.deepCopy(original), tracker);
		BitStructConnection<Root> root2 = bitser.createStructConnection(bitser.deepCopy(original), tracker);

		BitListConnection<Integer> integers1 = root1.getChildList("integers");
		integers1.addDelayed(5);
		tracker.applyChanges(root2);

		assertEquals(3, root2.state.integers.size());
		assertEquals(3, root2.state.integers.get(0));
		assertNull(root2.state.integers.get(1));
		assertEquals(5, root2.state.integers.get(2));

		BitListConnection<Integer> integers2 = root2.getChildList("integers");
		tracker.applyChanges(root1);
		integers2.addDelayed(12);
		tracker.applyChanges(root1);

		assertEquals(4, root1.state.integers.size());
		assertEquals(5, root1.state.integers.get(2));
		assertEquals(12, root1.state.integers.get(3));
	}

	@Test
	public void testStringList() throws IOException {
		Bitser bitser = new Bitser(false);
		TestBitStructConnection.ChangeTracker tracker = new TestBitStructConnection.ChangeTracker(1);

		BitStructConnection<Root> root1 = bitser.createStructConnection(new Root(), tracker);
		BitStructConnection<Root> root2 = bitser.createStructConnection(new Root(), tracker);

		BitListConnection<String> strings1 = root1.getChildList("strings");
		BitListConnection<String> strings2 = root2.getChildList("strings");

		strings1.addDelayed("hello");
		strings2.checkForChanges();
		tracker.applyChanges(root1);
		strings1.checkForChanges();
		tracker.applyChanges(root2);

		strings2.addDelayed(0, "hi");
		tracker.applyChanges(root1);
		tracker.applyChanges(root2);

		strings1.replaceDelayed(1, "world");
		tracker.applyChanges(root1);
		tracker.applyChanges(root2);

		strings2.removeDelayed(0);
		tracker.applyChanges(root1);
		tracker.applyChanges(root2);

		assertEquals(1, strings1.list.size());
		assertSame(strings1.list, root1.state.strings);
		assertEquals("world", strings1.list.get(0));
		assertEquals(strings1.list, strings2.list);
	}
}
