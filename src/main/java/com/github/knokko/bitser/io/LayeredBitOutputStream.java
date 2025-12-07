package com.github.knokko.bitser.io;

import java.io.IOException;
import java.io.OutputStream;

public class LayeredBitOutputStream extends BitOutputStream {

	private final BitOutputStream other;

	public LayeredBitOutputStream(OutputStream byteStream, BitOutputStream other) {
		super(byteStream);
		this.other = other;
	}

	@Override
	public boolean usesContextInfo() {
		return super.usesContextInfo() || other.usesContextInfo();
	}

	@Override
	public void write(boolean value) throws IOException {
		super.write(value);
		other.write(value);
	}

	@Override
	public void write(int value, int numBits) throws IOException {
		super.write(value, numBits);
		other.write(value, numBits);
	}

	@Override
	public void write(byte[] values) throws IOException {
		super.write(values);
		other.write(values);
	}

	@Override
	public void pushContext(String context, int counter) {
		super.pushContext(context, counter);
		other.pushContext(context, counter);
	}

	@Override
	public void popContext(String context, int counter) {
		super.popContext(context, counter);
		other.popContext(context, counter);
	}

	@Override
	public void prepareProperty(String fieldName, int counter) {
		super.prepareProperty(fieldName, counter);
		other.prepareProperty(fieldName, counter);
	}

	@Override
	public void finishProperty() {
		super.finishProperty();
		other.finishProperty();
	}
}
