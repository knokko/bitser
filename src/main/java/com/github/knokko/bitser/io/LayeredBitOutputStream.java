package com.github.knokko.bitser.io;

import java.io.IOException;
import java.io.OutputStream;

public class LayeredBitOutputStream extends BitOutputStream {

	private final BitOutputStream other;

	public LayeredBitOutputStream(OutputStream byteStream, BitOutputStream other) {
		super(byteStream);
		this.other = other;
	}

	public void write(boolean value) throws IOException {
		super.write(value);
		other.write(value);
	}

	@Override
	public void write(byte[] values) throws IOException {
		super.write(values);
		other.write(values);
	}
}
