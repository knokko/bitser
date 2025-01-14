package com.github.knokko.bitser.connection;

import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.Bitser;
import com.github.knokko.bitser.serialize.IntegerBitser;
import com.github.knokko.bitser.wrapper.BitserWrapper;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class BitServer<T> {

	public static <T> BitServer<T> tcp(Bitser bitser, T rootStruct, int port) throws IOException {
		@SuppressWarnings("resource") ServerSocket serverSocket = new ServerSocket(port);

		BitServer<T> server = new BitServer<T>(bitser, rootStruct, serverSocket.getLocalPort());

		new Thread(() -> {
			while (!serverSocket.isClosed()) {
				try {
					Socket next = serverSocket.accept();
					server.addConnection(next.getInputStream(), next.getOutputStream());
				} catch (IOException io) {
					System.out.println("Failed to open a connection with a client: " + io.getMessage());
				}
			}
			server.stop();
		}).start();

		return server;
	}

	private final Bitser bitser;
	private final BitserWrapper<T> rootWrapper;
	public final T rootStruct;
	private final Collection<Connection> connections = new ArrayList<>();
	private final BlockingQueue<List<BitStructChange>> changesToServer = new LinkedBlockingQueue<>();
	public final int port;

	public BitServer(Bitser bitser, T rootStruct, int port) {
		this.bitser = bitser;
		@SuppressWarnings("unchecked") Class<T> rootClass = (Class<T>) rootStruct.getClass();
		this.rootWrapper = bitser.cache.getWrapper(rootClass);
		this.rootStruct = rootStruct;
		this.port = port;

		new Thread(this::runUpdateLoop).start();
	}

	public void stop() {
		changesToServer.add(Collections.emptyList());
	}

	private void runUpdateLoop() {
		try {
			while (true) {
				List<BitStructChange> changes = changesToServer.take();
				if (changes.isEmpty()) break;

				synchronized (this) {
					for (Connection connection : connections) connection.changesToClient.add(changes);

					for (BitStructChange change : changes) {
						rootWrapper.handleChange(rootStruct, change, bitser.cache);
					}
				}
			}
		} catch (InterruptedException shouldNotHappen) {
			throw new RuntimeException(shouldNotHappen);
		}
	}

	private synchronized void addConnection(InputStream input, OutputStream output) throws IOException {
		byte[] encodedRootState = ConnectionHelper.encodePacket(bitser, rootStruct);
		connections.add(new Connection(new BitInputStream(input), new BitOutputStream(output), encodedRootState));
	}

	private class Connection {

		final BlockingQueue<List<BitStructChange>> changesToClient = new LinkedBlockingQueue<>();

		final BitInputStream input;
		final BitOutputStream output;
		byte[] encodedRootState;

		Connection(BitInputStream input, BitOutputStream output, byte[] encodedRootState) {
			this.input = input;
			this.output = output;
			this.encodedRootState = encodedRootState;

			new Thread(this::inputLoop).start();
			new Thread(this::outputLoop).start();
		}

		private void inputLoop() {
			try {
				while (true) {
					int packetSize = (int) IntegerBitser.decodeVariableInteger(0, Integer.MAX_VALUE, input);
					byte[] packetBytes = new byte[packetSize];
					input.read(packetBytes);
					BitPacket packet = bitser.deserialize(BitPacket.class, new BitInputStream(new ByteArrayInputStream(packetBytes)));
					System.out.println("Received packet " + packet.changes);
					changesToServer.add(packet.changes);
				}
			} catch (IOException io) {
				System.out.println("Connection input thread encountered IO exception: " + io.getMessage());
			}
		}

		private void outputLoop() {
			try {
				ConnectionHelper.sendEncodedPacket(encodedRootState, output);
				encodedRootState = null;
				while (true) {
					List<BitStructChange> nextChanges = changesToClient.take();
					// TODO Maybe poll ~1 ms for more?

					BitPacket packet = new BitPacket();
					packet.changes.addAll(nextChanges);

					ConnectionHelper.sendEncodedPacket(ConnectionHelper.encodePacket(bitser, packet), output);
				}
			} catch (IOException io) {
				System.out.println("Connection output thread encountered IO exception: " + io.getMessage());
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
