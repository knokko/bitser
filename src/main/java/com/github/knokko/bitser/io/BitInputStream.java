package com.github.knokko.bitser.io;

import com.github.knokko.bitser.RecursionNode;
import com.github.knokko.bitser.exceptions.UnexpectedBitserException;

import java.io.IOException;
import java.io.InputStream;

import static java.lang.Math.min;

public class BitInputStream {

	private final byte[] myBuffer = new byte[100];
	private final InputStream byteStream;
	private int bufferIndex;
	private int bufferLimit;
	private int currentByte;
	private int boolIndex = 8;

	public BitInputStream(InputStream byteStream) {
		this.byteStream = byteStream;
	}

	private void readNextByte() throws IOException {
		if (bufferIndex >= bufferLimit) {
			bufferLimit = byteStream.read(myBuffer);
			bufferIndex = 0;
			if (bufferLimit == -1) throw new IOException("End of stream reached");
		}
		currentByte = myBuffer[bufferIndex++] & 0xFF;
		boolIndex -= 8;
	}

	public boolean read() throws IOException {
		if (boolIndex == 8) readNextByte();
		return (currentByte & (1 << boolIndex++)) != 0;
	}

	public int read(int numBits) throws IOException {
		int value = (currentByte >> boolIndex) & ((1 << numBits) - 1);
		boolIndex += numBits;
		if (boolIndex > 8) {
			readNextByte();
			int remainingBits = boolIndex;
			int usedBits = numBits - remainingBits;
			value |= (currentByte & ((1 << remainingBits) - 1)) << usedBits;
		}
		return value;
	}

	public void read(byte[] destination) throws IOException {
		boolIndex = 8;

		int numReadBytes = min(bufferLimit - bufferIndex, destination.length);
		System.arraycopy(myBuffer, bufferIndex, destination, 0, numReadBytes);
		bufferIndex += numReadBytes;

		while (numReadBytes < destination.length) {
			int justReadBytes = byteStream.read(destination, numReadBytes, destination.length - numReadBytes);
			if (justReadBytes == -1) throw new IOException("End of stream reached");
			numReadBytes += justReadBytes;
			if (numReadBytes > destination.length) throw new UnexpectedBitserException("Too many bytes read?");
		}
	}

	public void close() throws IOException {
		byteStream.close();
	}

	public void pushContext(RecursionNode context, String fieldName) {}

	public void popContext(RecursionNode context, String fieldName) {}

	public void prepareProperty(String fieldName, int counter) {}

	public void finishProperty() {}
}
