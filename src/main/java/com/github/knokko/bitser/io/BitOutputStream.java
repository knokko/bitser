package com.github.knokko.bitser.io;

import java.io.IOException;
import java.io.OutputStream;

public class BitOutputStream {

	private final byte[] buffer = new byte[200];
	private final OutputStream byteStream;
	private int wipByte;
	private int bufferIndex;
	private int boolIndex;

	public BitOutputStream(OutputStream byteStream) {
		this.byteStream = byteStream;
	}

	public boolean usesContextInfo() {
		return false;
	}

	private void maybeFlushBuffer() throws IOException {
		if (bufferIndex == 200) {
			byteStream.write(buffer);
			bufferIndex = 0;
		}
	}

	private void flushCurrentByte() throws IOException {
		boolIndex = 0;
		buffer[bufferIndex++] = (byte) wipByte;
		wipByte = 0;
		maybeFlushBuffer();
	}

	public void write(boolean value) throws IOException {
		if (value) wipByte |= 1 << boolIndex;
		if (++boolIndex == 8) flushCurrentByte();
	}

	public void write(int value, int numBits) throws IOException {
		wipByte |= value << boolIndex;
		boolIndex += numBits;
		if (boolIndex >= 8) {
			buffer[bufferIndex++] = (byte) wipByte;
			boolIndex -= 8;
			wipByte = value >> (numBits - boolIndex);
			maybeFlushBuffer();
		}
	}

	private void flushBuffer() throws IOException {
		if (boolIndex != 0) flushCurrentByte();
		if (bufferIndex != 0) byteStream.write(buffer, 0, bufferIndex);
		bufferIndex = 0;
	}

	public void write(byte[] values) throws IOException {
		flushBuffer();
		byteStream.write(values);
	}

	public void finish() throws IOException {
		flushBuffer();
		byteStream.flush();
		byteStream.close();
	}

	public void pushContext(String context, int counter) {}

	public void popContext(String context, int counter) {}

	public void prepareProperty(String fieldName, int counter) {}

	public void finishProperty() {}
}
