package com.github.knokko.bitser.io;

import java.io.ByteArrayOutputStream;

public class BitCountStream extends BitOutputStream {

	private int counter;

	public BitCountStream() {
		super(new ByteArrayOutputStream());
	}

	@Override
	public void write(boolean value) {
		counter += 1;
	}

	@Override
	public void write(int value, int numBits) {
		counter += numBits;
	}

	@Override
	public void write(byte[] bytes) {
		int bytesSoFar = counter / 8;
		if (8 * bytesSoFar != counter) counter = 8 * (1 + bytesSoFar);
		counter += 8 * bytes.length;
	}

	public int getCounter() {
		return counter;
	}
}
