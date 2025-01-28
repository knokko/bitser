package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.field.*;
import com.github.knokko.bitser.io.BitCountStream;
import com.github.knokko.bitser.serialize.Bitser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.UUID;

import static com.github.knokko.bitser.wrapper.TestHelper.assertContains;
import static org.junit.jupiter.api.Assertions.*;

public class TestStableReferences {

	@BitStruct(backwardCompatible = false)
	private static class ItemType {

		@StableReferenceFieldId
		final UUID id = UUID.randomUUID();

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

		@ReferenceField(stable = true, label = "item types")
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

		ItemRoot loaded = new Bitser(false).deepCopy(root);

		assertEquals(2, loaded.types.size());
		assertEquals("sword", loaded.types.get(0).name);
		assertEquals(sword.id, loaded.types.get(0).id);
		assertEquals("shield", loaded.types.get(1).name);
		assertEquals(shield.id, loaded.types.get(1).id);
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

		@StableReferenceFieldId
		final UUID id = UUID.randomUUID();

		@IntegerField(minValue = 1, expectUniform = false)
		final int score;

		@ReferenceField(stable = true, label = "nodes")
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

		@ReferenceField(stable = true, label = "nodes")
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

		Graph loaded = new Bitser(false).deepCopy(graph);
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

		assertEquals(node1.id, loaded1.id);
		assertEquals(node2.id, loaded2.id);
		assertEquals(node3.id, loaded3.id);
		assertEquals(champion.id, loadedChampion.id);
		assertEquals(loser.id, loadedLoser.id);

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
	static class BothReferenceAndTarget {

		@SuppressWarnings("unused")
		@ReferenceField(stable = true, label = "test")
		@ReferenceFieldTarget(label = "test")
		final JustAnId id = new JustAnId();
	}

	@BitStruct(backwardCompatible = false)
	static class JustAnId {

		@SuppressWarnings("unused")
		@StableReferenceFieldId
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
	static class NonStructTarget {

		@SuppressWarnings("unused")
		@ReferenceFieldTarget(label = "test")
		final UUID id = UUID.randomUUID();

		@SuppressWarnings("unused")
		@ReferenceField(stable = true, label = "test")
		final UUID reference = id;
	}

	@Test
	public void testNonStructReferenceTarget() {
		String errorMessage =  assertThrows(
				InvalidBitFieldException.class,
				() -> new Bitser(true).serialize(new NonStructTarget(), new BitCountStream())
		).getMessage();
		assertContains(errorMessage, "Can't extract stable id from");
	}

	@BitStruct(backwardCompatible = false)
	static class WithoutStableId {

		@SuppressWarnings("unused")
		@BitField
		final boolean nope = true;
	}

	@BitStruct(backwardCompatible = false)
	static class StableReferenceTargetWithoutStableId {

		@SuppressWarnings("unused")
		@ReferenceFieldTarget(label = "test")
		WithoutStableId target;

		@ReferenceField(stable = true, label = "test")
		WithoutStableId reference;
	}

	@Test
	public void testStableReferenceTargetWithoutStableId() {
		StableReferenceTargetWithoutStableId target = new StableReferenceTargetWithoutStableId();
		target.target = new WithoutStableId();
		target.reference = target.target;
		String errorMessage = assertThrows(
				InvalidBitFieldException.class,
				() -> new Bitser(false).serialize(target, new BitCountStream())
		).getMessage();
		assertContains(errorMessage, "doesn't have an @StableReferenceFieldId");
	}

	@Test
	public void testNullReferenceTarget() {
		String errorMessage = assertThrows(
				InvalidBitValueException.class,
				() -> new Bitser(false).serialize(new StableReferenceTargetWithoutStableId(), new BitCountStream())
		).getMessage();
		assertContains(errorMessage, "must not be null");
	}

	@BitStruct(backwardCompatible = false)
	static class WithNullId {

		@SuppressWarnings("unused")
		@StableReferenceFieldId
		final UUID id = null;
	}

	@BitStruct(backwardCompatible = false)
	static class ReferencesNullId {

		@SuppressWarnings("unused")
		@ReferenceFieldTarget(label = "test")
		final WithNullId actualId = new WithNullId();

		@SuppressWarnings("unused")
		@ReferenceField(stable = true, label = "test")
		final WithNullId id = actualId;
	}

	@Test
	public void testReferenceToStableNullId() {
		String errorMessage = assertThrows(
				InvalidBitValueException.class,
				() -> new Bitser(true).serialize(new ReferencesNullId(), new BitCountStream())
		).getMessage();
		assertContains(errorMessage, "Stable UUID");
		assertContains(errorMessage, "is null");
	}

	@BitStruct(backwardCompatible = false)
	static class OptionalStableId {

		@SuppressWarnings("unused")
		@BitField(optional = true)
		@StableReferenceFieldId
		final UUID id = UUID.randomUUID();
	}

	@Test
	public void testOptionalStableReferenceId() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class,
				() -> new Bitser(false).serialize(new OptionalStableId(), new BitCountStream())
		).getMessage();
		assertContains(errorMessage, "@StableReferenceFieldId's can't be optional");
	}

	@BitStruct(backwardCompatible = false)
	static class StableNonUUID {

		@SuppressWarnings("unused")
		@StableReferenceFieldId
		final boolean id = false;
	}

	@Test
	public void testStableReferenceNonUUID() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class,
				() -> new Bitser(false).serialize(new StableNonUUID(), new BitCountStream())
		).getMessage();
		assertContains(errorMessage, "Only UUID fields can have @StableReferenceFieldId");
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
		assertContains(errorMessage, "Multiple stable targets have identity");
	}

	@BitStruct(backwardCompatible = false)
	static class MissingTargetLabel {

		@SuppressWarnings("unused")
		@ReferenceField(stable = true, label = "nope")
		final ItemType reference = new ItemType("hello");
	}

	@Test
	public void testMissingTargetLabel() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class,
				() -> new Bitser(false).serialize(new MissingTargetLabel(), new BitCountStream())
		).getMessage();

		assertContains(errorMessage, "Can't find @ReferenceFieldTarget with label nope");
	}
}
