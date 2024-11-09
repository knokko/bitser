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

	public int getCounter() {
		return counter;
	}
}
