package com.github.knokko.bitser.io;

public class BitStringStream extends BitOutputStream {

	private final StringBuilder bitString = new StringBuilder();

	public BitStringStream() {
		super(null);
	}

	@Override
	public void write(boolean value) {
		bitString.append(value ? '1' : '0');
	}

	@Override
	public void write(int value, int numBits) {
		for (int bit = 0; bit < numBits; bit++) write((value & (1 << bit)) != 0);
	}

	@Override
	public void finish() {
	}

	public String get() {
		return bitString.toString();
	}
}
