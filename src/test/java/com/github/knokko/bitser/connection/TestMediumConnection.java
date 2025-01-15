package com.github.knokko.bitser.connection;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.serialize.Bitser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;

import static com.github.knokko.bitser.connection.TestSimpleConnection.waitUntil;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestMediumConnection {

	@BitStruct(backwardCompatible = false)
	private static class Point {
		@BitField(ordering = 0)
		@IntegerField(expectUniform = false)
		int x;

		@BitField(ordering = 1)
		@IntegerField(expectUniform = false)
		int y;

		Point(int x, int y) {
			this.x = x;
			this.y = y;
		}

		@SuppressWarnings("unused")
		Point() {
			this(0, 0);
		}

		@Override
		public String toString() {
			return "Point(" + x + "," + y + ")";
		}
	}

	@BitStruct(backwardCompatible = false)
	private static class Root {
		@BitField(ordering = 0)
		Point origin;

		@SuppressWarnings("unused")
		@BitField(ordering = 1)
		ArrayList<Point> points = new ArrayList<>();
	}

	@Test
	public void testStructListConnection() throws IOException {
		int oldThreadCount = Thread.activeCount();

		Root serverStruct = new Root();
		serverStruct.origin = new Point(12, 34);

		Bitser bitser = new Bitser(true);
		BitServer<Root> server = BitServer.tcp(bitser, serverStruct, 0);
		serverStruct.origin.y += 1;

		BitClient<Root> client = BitClient.tcp(bitser, Root.class, "localhost", server.port);
		assertEquals(12, client.root.state.origin.x);
		assertEquals(35, client.root.state.origin.y);

		BitListConnection<Point> clientPoints = client.root.getChildList("points");
		clientPoints.addDelayed(new Point(100, 200));
		waitUntil(() -> clientPoints.list.size() == 1);
		assertEquals(100, clientPoints.list.get(0).x);

		BitClient<Root> client2 = BitClient.tcp(bitser, Root.class, "localhost", server.port);
		BitListConnection<Point> points2 = client2.root.getChildList("points");
		assertEquals(200, points2.list.get(0).y);
		assertEquals(35, client2.root.state.origin.y);

		points2.addDelayed(0, new Point(1, 2));
		waitUntil(() -> clientPoints.list.size() == 2);
		clientPoints.list.get(0).y += 3;
		clientPoints.list.get(0).x += 1;
		clientPoints.getChildStruct(0).checkForChanges();
		waitUntil(() -> points2.list.size() == 2 && points2.list.get(0).x == 2);
		assertEquals(5, points2.list.get(0).y);

		server.stop();
		waitUntil(() -> Thread.activeCount() == oldThreadCount);
	}
}
