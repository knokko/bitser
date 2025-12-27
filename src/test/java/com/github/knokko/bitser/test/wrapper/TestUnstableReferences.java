package com.github.knokko.bitser.test.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.exceptions.ReferenceBitserException;
import com.github.knokko.bitser.field.*;
import com.github.knokko.bitser.io.BitCountStream;
import com.github.knokko.bitser.Bitser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import static com.github.knokko.bitser.test.wrapper.TestHelper.assertContains;
import static org.junit.jupiter.api.Assertions.*;

public class TestUnstableReferences {

	@BitStruct(backwardCompatible = false)
	private static class ItemType {

		@StringField
		final String name;

		@SuppressWarnings("unused")
		ItemType() {
			this(null);
		}

		ItemType(String name) {
			this.name = name;
		}
	}

	@BitStruct(backwardCompatible = false)
	private static class Item {

		@StringField
		final String name;

		@ReferenceField(stable = false, label = "item types")
		final ItemType type;

		@SuppressWarnings("unused")
		Item() {
			this(null, null);
		}

		Item(String name, ItemType type) {
			this.name = name;
			this.type = type;
		}
	}

	@BitStruct(backwardCompatible = false)
	private static class ItemRoot {

		@BitField
		final ArrayList<Item> items = new ArrayList<>();

		@ReferenceFieldTarget(label = "item types")
		final ArrayList<ItemType> types = new ArrayList<>();
	}

	@Test
	public void testItems() {

		ItemRoot root = new ItemRoot();
		ItemType sword = new ItemType("sword");
		ItemType shield = new ItemType("shield");

		root.types.add(sword);
		root.types.add(shield);

		root.items.add(new Item("rusty sword", sword));
		root.items.add(new Item("cold shield", shield));
		root.items.add(new Item("spiky shield", shield));

		ItemRoot loaded = new Bitser(false).stupidDeepCopy(root);

		assertEquals(2, loaded.types.size());
		assertEquals("sword", loaded.types.get(0).name);
		assertEquals("shield", loaded.types.get(1).name);
		assertEquals(3, loaded.items.size());
		assertEquals("rusty sword", loaded.items.get(0).name);
		assertEquals("cold shield", loaded.items.get(1).name);
		assertEquals("spiky shield", loaded.items.get(2).name);
		assertSame(loaded.types.get(0), loaded.items.get(0).type);
		assertSame(loaded.types.get(1), loaded.items.get(1).type);
		assertSame(loaded.types.get(1), loaded.items.get(2).type);
	}

	@BitStruct(backwardCompatible = false)
	static class Node {

		@IntegerField(minValue = 1, expectUniform = false)
		final int score;

		@ReferenceField(stable = false, label = "nodes")
		final ArrayList<Node> neighbours = new ArrayList<>();

		@BitField(optional = true)
		DeepNodeReference bestFriend;

		Node(int score) {
			this.score = score;
		}

		@SuppressWarnings("unused")
		Node() {
			this(0);
		}
	}

	@BitStruct(backwardCompatible = false)
	static class DeepNodeReference {

		@ReferenceField(stable = false, label = "nodes")
		final Node node;

		DeepNodeReference(Node node) {
			this.node = node;
		}

		@SuppressWarnings("unused")
		DeepNodeReference() {
			this(null);
		}
	}

	@BitStruct(backwardCompatible = false)
	static class DeepNodeTarget {

		@ReferenceFieldTarget(label = "nodes")
		final Node node;

		DeepNodeTarget(Node node) {
			this.node = node;
		}

		@SuppressWarnings("unused")
		DeepNodeTarget() {
			this(null);
		}
	}

	@BitStruct(backwardCompatible = false)
	static class Graph {

		@ReferenceFieldTarget(label = "nodes")
		final ArrayList<Node> mostNodes = new ArrayList<>();

		@BitField
		DeepNodeReference champion;

		@BitField
		DeepNodeTarget loser;
	}

	@Test
	public void testGraph() {
		Graph graph = new Graph();

		Node champion = new Node(100);
		Node loser = new Node(1);
		Node node1 = new Node(10);
		Node node2 = new Node(20);
		Node node3 = new Node(30);

		graph.mostNodes.add(node1);
		graph.mostNodes.add(node2);
		graph.mostNodes.add(node3);
		graph.mostNodes.add(champion);
		graph.champion = new DeepNodeReference(champion);
		graph.loser = new DeepNodeTarget(loser);

		champion.neighbours.add(node2);
		node2.neighbours.add(champion);
		node2.neighbours.add(node3);
		node1.bestFriend = new DeepNodeReference(loser);
		node3.bestFriend = new DeepNodeReference(node2);
		node3.neighbours.add(node1);

		Graph loaded = new Bitser(false).stupidDeepCopy(graph);
		assertEquals(4, loaded.mostNodes.size());

		Node loaded1 = loaded.mostNodes.get(0);
		Node loaded2 = loaded.mostNodes.get(1);
		Node loaded3 = loaded.mostNodes.get(2);
		Node loadedChampion = loaded.mostNodes.get(3);
		Node loadedLoser = loaded.loser.node;

		// Sanity checks
		assertNotSame(loaded1, loaded2);
		assertNotSame(loaded2, loaded3);
		assertNotSame(loadedChampion, loadedLoser);
		assertSame(loadedChampion, loaded.champion.node);

		assertEquals(100, loadedChampion.score);
		assertEquals(1, loadedLoser.score);
		assertEquals(10, loaded1.score);
		assertEquals(20, loaded2.score);
		assertEquals(30, loaded3.score);

		assertSame(loaded1.bestFriend.node, loadedLoser);
		assertSame(loaded3.bestFriend.node, loaded2);

		assertEquals(2, loaded2.neighbours.size());
		assertSame(loadedChampion, loaded2.neighbours.get(0));
		assertSame(loaded3, loaded2.neighbours.get(1));
		assertEquals(1, loadedChampion.neighbours.size());
		assertSame(loaded2, loadedChampion.neighbours.get(0));
		assertEquals(1, loaded3.neighbours.size());
		assertSame(loaded1, loaded3.neighbours.get(0));
	}

	@BitStruct(backwardCompatible = false)
	static class NonStructReferences {

		@ReferenceFieldTarget(label = "no struct")
		final String hello;

		@SuppressWarnings("unused")
		@ReferenceField(stable = false, label = "no struct")
		final String hi;

		NonStructReferences(String hello) {
			this.hello = hello;
			this.hi = this.hello;
		}

		@SuppressWarnings("unused")
		NonStructReferences() {
			this(null);
		}
	}

	@Test
	public void testNonBitStructReferences() {
		NonStructReferences loaded = new Bitser(true).stupidDeepCopy(new NonStructReferences("world"));
		assertEquals("world", loaded.hello);
		assertSame(loaded.hello, loaded.hi);
	}

	@Test
	public void testReferenceOutsideRoot() {
		ItemRoot root = new ItemRoot();
		root.items.add(new Item("item", new ItemType("outside")));

		String errorMessage = assertThrows(
				ReferenceBitserException.class,
				() -> new Bitser(false).serialize(root, new BitCountStream())
		).getMessage();

		assertContains(errorMessage, "Can't find @ReferenceFieldTarget with label item types");
	}

	@BitStruct(backwardCompatible = false)
	static class MissingTargetLabel {

		@SuppressWarnings("unused")
		@ReferenceField(stable = false, label = "nope")
		final String reference = "hello";
	}

	@Test
	public void testMissingTargetLabel() {
		String errorMessage = assertThrows(
				ReferenceBitserException.class,
				() -> new Bitser(false).serialize(new MissingTargetLabel(), new BitCountStream())
		).getMessage();

		assertContains(errorMessage, "Can't find @ReferenceFieldTarget with label nope");
	}

	@BitStruct(backwardCompatible = false)
	static class BothReferenceAndTarget {

		@SuppressWarnings("unused")
		@ReferenceField(stable = false, label = "test")
		@ReferenceFieldTarget(label = "test")
		final JustAnId id = new JustAnId();
	}

	@BitStruct(backwardCompatible = false)
	static class JustAnId {

		@SuppressWarnings("unused")
		@BitField
		final UUID id = UUID.randomUUID();
	}

	@Test
	public void testFieldThatIsBothReferenceAndTarget() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class,
				() -> new Bitser(true).serialize(new BothReferenceAndTarget(), new BitCountStream())
		).getMessage();
		assertContains(errorMessage, "is both a reference field and a reference target");
	}

	@BitStruct(backwardCompatible = false)
	static class WithPrimitiveTarget {

		@SuppressWarnings("unused")
		@IntegerField(expectUniform = false)
		@ReferenceFieldTarget(label = "invalid")
		final int test = 1234;
	}

	@Test
	public void testPrimitiveTarget() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class,
				() -> new Bitser(true).serialize(new WithPrimitiveTarget(), new BitCountStream())
		).getMessage();
		assertContains(errorMessage, "is primitive, which is forbidden");
	}

	@Test
	public void testMultipleTargetsWithTheSameIdentity() {
		ItemRoot root = new ItemRoot();
		ItemType type = new ItemType("dual");
		root.types.add(type);
		root.types.add(type);

		String errorMessage = assertThrows(
				ReferenceBitserException.class,
				() -> new Bitser(false).serialize(root, new BitCountStream())
		).getMessage();
		assertContains(errorMessage, "Multiple unstable targets have identity");
		assertContains(errorMessage, "ItemRoot -> types");
	}

	@BitStruct(backwardCompatible = false)
	static class StableTarget {

		@SuppressWarnings("unused")
		@StableReferenceFieldId
		final UUID id = UUID.randomUUID();

		@BitField
		String hello;
	}

	@BitStruct(backwardCompatible = false)
	static class Mixed {

		@ReferenceFieldTarget(label = "hi")
		StableTarget target1;

		@ReferenceFieldTarget(label = "hi")
		StableTarget target2;

		@ReferenceField(stable = true, label = "hi")
		StableTarget stableReference;

		@ReferenceField(stable = false, label = "hi")
		StableTarget unstableReference1;

		@ReferenceField(stable = false, label = "hi")
		StableTarget unstableReference2;
	}

	@Test
	public void testMixStableAndUnstableReferences() {
		Mixed mixed = new Mixed();
		mixed.target1 = new StableTarget();
		mixed.target1.hello = "world";
		mixed.target2 = new StableTarget();
		mixed.target2.hello = "triangle";

		mixed.stableReference = mixed.target2;
		mixed.unstableReference1 = mixed.target1;
		mixed.unstableReference2 = mixed.target2;

		Mixed loaded = new Bitser(false).stupidDeepCopy(mixed);
		assertEquals("world", loaded.target1.hello);
		assertEquals("triangle", loaded.target2.hello);
		assertSame(loaded.target1, loaded.unstableReference1);
		assertSame(loaded.target2, loaded.stableReference);
		assertSame(loaded.target2, loaded.unstableReference2);
	}

	@Test
	public void testMissingLegacyTargetLabel1() {
		ItemType itemType = new ItemType("bye bye");

		ItemRoot primaryRoot = new ItemRoot();
		primaryRoot.items.add(new Item("it", itemType));

		ItemRoot withRoot = new ItemRoot();
		withRoot.types.add(itemType);

		Bitser bitser = new Bitser(false);
		byte[] bytes = bitser.toBytes(primaryRoot, withRoot);

		String errorMessage = assertThrows(
				ReferenceBitserException.class,
				() -> bitser.fromBytes(ItemRoot.class, bytes)
		).getMessage();
		assertContains(errorMessage, "Can't find @ReferenceFieldTarget with label item types");
		assertContains(errorMessage, "-> type");
	}

	@Test
	public void testMissingLegacyTargetLabel2() {
		ItemType itemType = new ItemType("bye bye");

		Item item = new Item("it", itemType);

		ItemRoot with = new ItemRoot();
		with.types.add(itemType);

		Bitser bitser = new Bitser(false);
		byte[] bytes = bitser.toBytes(item, with);

		String errorMessage = assertThrows(
				ReferenceBitserException.class,
				() -> bitser.fromBytes(Item.class, bytes)
		).getMessage();
		assertContains(errorMessage, "Can't find @ReferenceFieldTarget with label item types");
		assertContains(errorMessage, "-> type");
	}

	@BitStruct(backwardCompatible = true)
	private static class RequiredReferenceList {

		@BitField(id = 0)
		@ReferenceFieldTarget(label = "label")
		final String target = "abc";

		@BitField(id = 1)
		@SuppressWarnings("unused")
		@ReferenceField(stable = false, label = "label")
		String[] references = { target, null };
	}

	@Test
	public void testForbidNullReferences() {
		String errorMessage = assertThrows(
				InvalidBitValueException.class,
				() -> new Bitser(true).toBytes(new RequiredReferenceList())
		).getMessage();
		assertContains(errorMessage, "RequiredReferenceList -> references");
		assertContains(errorMessage, "must not have null elements");
	}

	@BitStruct(backwardCompatible = true)
	private static class GenericTarget<T> {

		@SuppressWarnings("unused")
		T defaultValue;
	}

	@BitStruct(backwardCompatible = true)
	private static class GenericRoot {

		@BitField(id = 0)
		@ReferenceFieldTarget(label = "generic1")
		GenericTarget<?> target;

		@BitField(id = 1)
		@ReferenceField(stable = false, label = "generic1")
		GenericTarget<?> reference;

		@BitField(id = 2)
		@ReferenceFieldTarget(label = "generic2")
		GenericTarget<?>[] targets;

		@BitField(id = 3)
		@ReferenceField(stable = false, label = "generic2")
		GenericTarget<?>[] references;

		@BitField(id = 4)
		@ReferenceFieldTarget(label = "generic3")
		ArrayList<GenericTarget<?>> targetList = new ArrayList<>();

		@BitField(id = 5)
		@ReferenceField(stable = false, label = "generic3")
		ArrayList<GenericTarget<?>> referenceList = new ArrayList<>();

		@BitField(id = 6)
		@NestedFieldSetting(path = "k", fieldName = "TARGET")
		@NestedFieldSetting(path = "v", fieldName = "TARGET")
		HashMap<GenericTarget<?>, GenericTarget<?>> targetMap = new HashMap<>();

		@BitField(id = 7)
		@NestedFieldSetting(path = "k", fieldName = "REFERENCE")
		@NestedFieldSetting(path = "v", fieldName = "REFERENCE")
		HashMap<GenericTarget<?>, GenericTarget<?>> referenceMap = new HashMap<>();

		@SuppressWarnings("unused")
		@ReferenceFieldTarget(label = "generic4")
		private static final boolean TARGET = false;

		@SuppressWarnings("unused")
		@ReferenceField(stable = false, label = "generic4")
		private static final boolean REFERENCE = false;
	}

	@Test
	public void testGenericTarget() {
		Bitser bitser = new Bitser(false);
		GenericRoot original = new GenericRoot();
		original.target = new GenericTarget<String>();
		original.reference = original.target;
		original.targets = new GenericTarget[] { new GenericTarget<Integer>() };
		original.references = new GenericTarget[] { original.targets[0] };
		original.targetList.add(new GenericTarget<Double>());
		original.referenceList.add(original.targetList.get(0));
		original.targetMap.put(new GenericTarget<Long>(), new GenericTarget<Class<?>>());
		original.referenceMap.put(
				original.targetMap.keySet().iterator().next(),
				original.targetMap.values().iterator().next()
		);

		GenericRoot loaded = bitser.stupidDeepCopy(original);
		assertSame(loaded.target, loaded.reference);
		assertSame(loaded.targets[0], loaded.references[0]);
		assertSame(loaded.targetList.get(0), loaded.referenceList.get(0));
		assertSame(loaded.targetMap.keySet().iterator().next(), loaded.referenceMap.keySet().iterator().next());
		assertSame(loaded.targetMap.values().iterator().next(), loaded.referenceMap.values().iterator().next());

		GenericRoot backward = bitser.stupidDeepCopy(original, Bitser.BACKWARD_COMPATIBLE);
		assertSame(backward.target, backward.reference);
		assertSame(backward.targets[0], backward.references[0]);
		assertSame(backward.targetList.get(0), backward.referenceList.get(0));
		assertSame(backward.targetMap.keySet().iterator().next(), backward.referenceMap.keySet().iterator().next());
		assertSame(backward.targetMap.values().iterator().next(), backward.referenceMap.values().iterator().next());
	}
}
