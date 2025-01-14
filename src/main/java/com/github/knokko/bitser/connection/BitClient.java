package com.github.knokko.bitser.connection;

import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.Bitser;
import com.github.knokko.bitser.serialize.IntegerBitser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class BitClient<T> {

	public static <T> BitClient<T> tcp(Bitser bitser, Class<T> rootStructClass, String host, int port) throws IOException {
		@SuppressWarnings("resource") Socket socket = new Socket(host, port);

		BitInputStream input = new BitInputStream(socket.getInputStream());
		int rootByteSize = (int) IntegerBitser.decodeVariableInteger(0, Integer.MAX_VALUE, input);
		byte[] rootBytes = new byte[rootByteSize];
		input.read(rootBytes);
		T rootStruct = bitser.deserialize(rootStructClass, new BitInputStream(new ByteArrayInputStream(rootBytes)));

		return new BitClient<>(bitser, rootStruct, input, new BitOutputStream(socket.getOutputStream()), socket::close);
	}

	@FunctionalInterface
	public interface CloseMethod {

		void close() throws IOException;
	}

	private final Bitser bitser;
	public final BitStructConnection<T> root;
	private final BlockingQueue<List<BitStructChange>> pendingChanges = new LinkedBlockingQueue<>();
	private final BitInputStream input;
	private final BitOutputStream output;
	private final CloseMethod close;

	public BitClient(Bitser bitser, T rootStruct, BitInputStream input, BitOutputStream output, CloseMethod close) {
		this.bitser = bitser;
		this.root = new BitStructConnection<>(bitser, rootStruct, pendingChanges::add);
		this.input = input;
		this.output = output;
		this.close = close;

		new Thread(this::inputThread).start();
		new Thread(this::outputThread).start();
	}

	private void inputThread() {
		try {
			while (true) {
				int packetSize = (int) IntegerBitser.decodeVariableInteger(0, Integer.MAX_VALUE, input);
				byte[] packetBytes = new byte[packetSize];
				input.read(packetBytes);
				BitPacket packet = bitser.deserialize(BitPacket.class, new BitInputStream(new ByteArrayInputStream(packetBytes)));
				root.handleChanges(packet.changes);
			}
		} catch (IOException io) {
			System.out.println("Client input thread encountered IO exception: " + io.getMessage());
		}
	}

	private void outputThread() {
		try {
			while (true) {
				List<BitStructChange> nextChanges = pendingChanges.take();
				if (nextChanges.isEmpty()) return;
				// TODO Maybe poll ~1 ms for more?

				BitPacket packet = new BitPacket();
				packet.changes.addAll(nextChanges);
				ConnectionHelper.sendEncodedPacket(ConnectionHelper.encodePacket(bitser, packet), output);
			}
		} catch (IOException io) {
			System.out.println("Client output thread encountered IO exception: " + io.getMessage());
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				close.close();
			} catch (IOException alreadyClosed) {
				// Already closed, nothing more we can do
			}
		}
	}

	public void close() {
		pendingChanges.add(Collections.emptyList());
	}
}
