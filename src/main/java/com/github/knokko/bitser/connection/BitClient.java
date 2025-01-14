package com.github.knokko.bitser.connection;

import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.Bitser;
import com.github.knokko.bitser.serialize.IntegerBitser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static com.github.knokko.bitser.connection.ConnectionHelper.STOP_SIGN;

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

	public final BitStructConnection<T> root;
	private final BlockingQueue<byte[]> pendingChanges = new LinkedBlockingQueue<>();
	private final BitInputStream input;
	private final BitOutputStream output;
	private final CloseMethod close;

	public BitClient(Bitser bitser, T rootStruct, BitInputStream input, BitOutputStream output, CloseMethod close) {
		this.root = new BitStructConnection<>(bitser, rootStruct, listener -> {
			ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
			BitOutputStream bitOutput = new BitOutputStream(byteOutput);
			try {
				listener.report(bitOutput);
				bitOutput.finish();
			} catch (IOException shouldNotHappen) {
				throw new Error(shouldNotHappen);
			}
			pendingChanges.add(byteOutput.toByteArray());
		});
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
				root.handleChanges(new BitInputStream(new ByteArrayInputStream(packetBytes)));
			}
		} catch (IOException io) {
			System.out.println("Client input thread encountered IO exception: " + io.getMessage());
		}
	}

	private void outputThread() {
		try {
			while (true) {
				byte[] nextChanges = pendingChanges.take();
				if (nextChanges == STOP_SIGN) return;
				ConnectionHelper.sendEncodedPacket(nextChanges, output);
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
		pendingChanges.add(STOP_SIGN);
	}
}
