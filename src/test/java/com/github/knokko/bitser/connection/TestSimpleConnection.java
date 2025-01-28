package com.github.knokko.bitser.connection;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.FloatField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.serialize.Bitser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.function.BooleanSupplier;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestSimpleConnection {

	@BitStruct(backwardCompatible = false)
	private static class ExampleStruct {

		@BitField
		String a = "a";

		@BitField
		boolean b = true;

		@IntegerField(expectUniform = false)
		int x = 1;

		@FloatField
		double y = 2.5;
	}

	static void waitUntil(BooleanSupplier isFinished) {
		for (int counter = 0; counter < 100; counter++) {
			if (isFinished.getAsBoolean()) return;
			try {
				sleep(10);
			} catch (InterruptedException shouldNotHappen) {
				throw new RuntimeException(shouldNotHappen);
			}
		}
		throw new RuntimeException("Timeout reached");
	}

	@Test
	public void testSimpleStructConnection() throws IOException {
		int oldThreadCount = Thread.activeCount();

		ExampleStruct serverStruct = new ExampleStruct();
		serverStruct.y = 5.5;

		Bitser bitser = new Bitser(true);
		BitServer<ExampleStruct> server = BitServer.tcp(bitser, serverStruct, 0);
		serverStruct.x = 10;
		serverStruct.a = "b";

		BitClient<ExampleStruct> client = BitClient.tcp(bitser, ExampleStruct.class, "localhost", server.port);
		assertEquals("b", client.root.state.a);
		assertTrue(client.root.state.b);
		assertEquals(10, client.root.state.x);
		assertEquals(5.5, client.root.state.y);

		BitClient<ExampleStruct> client2 = BitClient.tcp(bitser, ExampleStruct.class, "localhost", server.port);
		assertTrue(client2.root.state.b);

		client.root.state.a = "hello";
		client.root.state.b = false;
		client.root.checkForChanges();

		waitUntil(() -> !serverStruct.b);
		assertEquals("hello", serverStruct.a);

		waitUntil(() -> !client2.root.state.b);
		assertEquals("hello", client2.root.state.a);

		client2.root.state.x = 21;
		client2.root.state.y = 21.0;
		client2.root.checkForChanges();
		client2.close();

		waitUntil(() -> client.root.state.x == 21);
		assertEquals(21.0, client.root.state.y);

		server.stop();

		waitUntil(() -> Thread.activeCount() == oldThreadCount);
	}
}
