package com.github.knokko.bitser.io;

import java.io.IOException;
import java.io.InputStream;

public class BitInputStream {

	private final InputStream byteStream;
	private int currentByte;
	private int boolIndex = 8;

	public BitInputStream(InputStream byteStream) {
		this.byteStream = byteStream;
	}

	public boolean read() throws IOException {
		if (boolIndex == 8) {
			currentByte = byteStream.read();
			if (currentByte == -1) {
				throw new IOException("End of stream reached");
			}
			boolIndex = 0;
		}

		return (currentByte & (1 << boolIndex++)) != 0;
	}

	public void read(byte[] destination) throws IOException {
		boolIndex = 8;

		int numReadBytes = 0;
		while (numReadBytes < destination.length) {
			int justReadBytes = byteStream.read(destination, numReadBytes, destination.length - numReadBytes);
			if (justReadBytes == -1) throw new IOException("End of stream reached");
			numReadBytes += justReadBytes;
			if (numReadBytes > destination.length) throw new Error("Too many bytes read?");
		}
	}

	public void close() throws IOException {
		byteStream.close();
	}
}
