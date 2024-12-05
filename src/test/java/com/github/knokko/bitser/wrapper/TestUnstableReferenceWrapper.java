package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.field.*;
import com.github.knokko.bitser.io.BitCountStream;
import com.github.knokko.bitser.io.BitserHelper;
import com.github.knokko.bitser.serialize.Bitser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TestUnstableReferenceWrapper {

	@BitStruct(backwardCompatible = false)
	private static class ItemType {

		@BitField(ordering = 0)
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

		@BitField(ordering = 0)
		@StringField
		final String name;

		@BitField(ordering = 1)
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

		@BitField(ordering = 0)
		@CollectionField
		final ArrayList<Item> items = new ArrayList<>();

		@BitField(ordering = 1)
		@CollectionField
		@ReferenceFieldTarget(label = "item types")
		final ArrayList<ItemType> types = new ArrayList<>();
	}

	@Test
	public void testItems() throws IOException {

		ItemRoot root = new ItemRoot();
		ItemType sword = new ItemType("sword");
		ItemType shield = new ItemType("shield");

		root.types.add(sword);
		root.types.add(shield);

		root.items.add(new Item("rusty sword", sword));
		root.items.add(new Item("cold shield", shield));
		root.items.add(new Item("spiky shield", shield));

		ItemRoot loaded = BitserHelper.serializeAndDeserialize(new Bitser(false), root);

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

		@BitField(ordering = 0)
		@IntegerField(minValue = 1, expectUniform = false)
		final int score;

		@BitField(ordering = 1)
		@CollectionField
		@ReferenceField(stable = false, label = "nodes")
		final ArrayList<Node> neighbours = new ArrayList<>();

		@BitField(ordering = 2, optional = true)
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

		@BitField(ordering = 0)
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

		@BitField(ordering = 0)
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

		@BitField(ordering = 0)
		@CollectionField
		@ReferenceFieldTarget(label = "nodes")
		final ArrayList<Node> mostNodes = new ArrayList<>();

		@BitField(ordering = 1)
		DeepNodeReference champion;

		@BitField(ordering = 2)
		DeepNodeTarget loser;
	}

	@Test
	public void testGraph() throws IOException {
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

		Graph loaded = BitserHelper.serializeAndDeserialize(new Bitser(false), graph);
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

		@BitField(ordering = 0)
		@ReferenceFieldTarget(label = "no struct")
		final String hello;

		@SuppressWarnings("unused")
		@BitField(ordering = 1)
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
	public void testNonBitStructReferences() throws IOException {
		NonStructReferences loaded = BitserHelper.serializeAndDeserialize(
				new Bitser(true), new NonStructReferences("world")
		);
		assertEquals("world", loaded.hello);
		assertSame(loaded.hello, loaded.hi);
	}

	@Test
	public void testReferenceOutsideRoot() {
		ItemRoot root = new ItemRoot();
		root.items.add(new Item("item", new ItemType("outside")));

		String errorMessage = assertThrows(
				InvalidBitValueException.class,
				() -> new Bitser(false).serialize(root, new BitCountStream())
		).getMessage();

		assertTrue(
				errorMessage.contains("Can't find unstable reference target with label item types"),
				"Expected " + errorMessage + " to contain \"Can't find unstable reference target with label item types\""
		);
	}

	@BitStruct(backwardCompatible = false)
	static class MissingTargetLabel {

		@SuppressWarnings("unused")
		@BitField(ordering = 0)
		@ReferenceField(stable = false, label = "nope")
		final String reference = "hello";
	}

	@Test
	public void testMissingTargetLabel() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class,
				() -> new Bitser(false).serialize(new MissingTargetLabel(), new BitCountStream())
		).getMessage();

		assertTrue(
				errorMessage.contains("Can't find @ReferenceFieldTarget with label nope"),
				"Expected " + errorMessage + " to contain \"Can't find @ReferenceFieldTarget with label nope\""
		);
	}

	@BitStruct(backwardCompatible = false)
	static class BothReferenceAndTarget {

		@SuppressWarnings("unused")
		@BitField(ordering = 0)
		@ReferenceField(stable = false, label = "test")
		@ReferenceFieldTarget(label = "test")
		final JustAnId id = new JustAnId();
	}

	@BitStruct(backwardCompatible = false)
	static class JustAnId {

		@SuppressWarnings("unused")
		@BitField(ordering = 0)
		final UUID id = UUID.randomUUID();
	}

	@Test
	public void testFieldThatIsBothReferenceAndTarget() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class,
				() -> new Bitser(true).serialize(new BothReferenceAndTarget(), new BitCountStream())
		).getMessage();
		assertTrue(
				errorMessage.contains("is both a reference field and a reference target"),
				"Expected " + errorMessage + " to contain \"is both a reference field and a reference target\""
		);
	}

	@BitStruct(backwardCompatible = false)
	static class WithPrimitiveTarget {

		@SuppressWarnings("unused")
		@BitField(ordering = 0)
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
		assertTrue(
				errorMessage.contains("is primitive, which is forbidden"),
				"Expected " + errorMessage + " to contain \"is primitive, which is forbidden\""
		);
	}

	@Test
	public void testMultipleTargetsWithTheSameIdentity() {
		ItemRoot root = new ItemRoot();
		ItemType type = new ItemType("dual");
		root.types.add(type);
		root.types.add(type);

		String errorMessage = assertThrows(
				InvalidBitValueException.class,
				() -> new Bitser(false).serialize(root, new BitCountStream())
		).getMessage();
		assertTrue(
				errorMessage.contains("Multiple unstable targets have identity"),
				"Expected " + errorMessage + " to contain \"Multiple unstable targets have identity\""
		);
	}

	@BitStruct(backwardCompatible = false)
	static class StableTarget {

		@SuppressWarnings("unused")
		@BitField(ordering = 0)
		@StableReferenceFieldId
		final UUID id = UUID.randomUUID();

		@BitField(ordering = 1)
		String hello;
	}

	@BitStruct(backwardCompatible = false)
	static class Mixed {

		@BitField(ordering = 0)
		@ReferenceFieldTarget(label = "hi")
		StableTarget target1;

		@BitField(ordering = 1)
		@ReferenceFieldTarget(label = "hi")
		StableTarget target2;

		@BitField(ordering = 2)
		@ReferenceField(stable = true, label = "hi")
		StableTarget stableReference;

		@BitField(ordering = 3)
		@ReferenceField(stable = false, label = "hi")
		StableTarget unstableReference1;

		@BitField(ordering = 4)
		@ReferenceField(stable = false, label = "hi")
		StableTarget unstableReference2;
	}

	@Test
	public void testMixStableAndUnstableReferences() throws IOException {
		Mixed mixed = new Mixed();
		mixed.target1 = new StableTarget();
		mixed.target1.hello = "world";
		mixed.target2 = new StableTarget();
		mixed.target2.hello = "triangle";

		mixed.stableReference = mixed.target2;
		mixed.unstableReference1 = mixed.target1;
		mixed.unstableReference2 = mixed.target2;

		Mixed loaded = BitserHelper.serializeAndDeserialize(new Bitser(false), mixed);
		assertEquals("world", loaded.target1.hello);
		assertEquals("triangle", loaded.target2.hello);
		assertSame(loaded.target1, loaded.unstableReference1);
		assertSame(loaded.target2, loaded.stableReference);
		assertSame(loaded.target2, loaded.unstableReference2);
	}
}
