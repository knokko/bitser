package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.serialize.Bitser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TestLinkedLists {

	@BitStruct(backwardCompatible = true)
	private static class BitLinkedList {

		@BitField(id = 0)
		String value = "";

		@BitField(id = 1, optional = true)
		BitLinkedList next;
	}

	@Test
	public void testShort() {
		Bitser bitser = new Bitser(false);

		BitLinkedList root = new BitLinkedList();
		root.value = "hello";
		root.next = new BitLinkedList();
		root.next.value = "world";

		BitLinkedList recovered = bitser.deepCopy(root);
		assertEquals("hello", recovered.value);
		assertEquals("world", recovered.next.value);
		assertNull(recovered.next.next);
	}

	@Test
	public void testShortBackwardCompatible() {
		Bitser bitser = new Bitser(false);

		BitLinkedList root = new BitLinkedList();
		root.value = "hello";
		root.next = new BitLinkedList();
		root.next.value = "world";

		BitLinkedList recovered = bitser.deepCopy(root, Bitser.BACKWARD_COMPATIBLE);
		assertEquals("hello", recovered.value);
		assertEquals("world", recovered.next.value);
		assertNull(recovered.next.next);
	}
}
