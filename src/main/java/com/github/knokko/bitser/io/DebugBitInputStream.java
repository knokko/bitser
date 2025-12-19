package com.github.knokko.bitser.io;

import com.github.knokko.bitser.RecursionNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Arrays;

public class DebugBitInputStream extends BitInputStream {

	private final PrintWriter writer;
	private final boolean aggressiveFlush;

	public DebugBitInputStream(InputStream byteStream, PrintWriter writer, boolean aggressiveFlush) {
		super(byteStream);
		this.writer = writer;
		this.aggressiveFlush = aggressiveFlush;
	}

	public boolean read() throws IOException {
		boolean result = super.read();
		writer.print(result ? '1' : '0');
		return result;
	}

	public int read(int numBits) throws IOException {
		int result = super.read(numBits);
		for (int bit = 0; bit < numBits; bit++) writer.print((result & (1 << bit)) != 0 ? '1' : '0');
		return result;
	}

	@Override
	public void read(byte[] destination) throws IOException {
		super.read(destination);
		writer.print(Arrays.toString(destination));
	}

	@Override
	public void pushContext(RecursionNode context, String fieldName) {
		super.pushContext(context, fieldName);
		writer.println("begin " + context.generateTrace(fieldName));
		if (aggressiveFlush) writer.flush();
	}

	@Override
	public void popContext(RecursionNode context, String fieldName) {
		super.popContext(context, fieldName);
		writer.println("end " + context.generateTrace(fieldName));
		if (aggressiveFlush) writer.flush();
	}

	@Override
	public void prepareProperty(String fieldName, int counter) {
		super.prepareProperty(fieldName, counter);
		writer.print(fieldName + ": ");
		if (aggressiveFlush) writer.flush();
	}

	@Override
	public void finishProperty() {
		super.finishProperty();
		writer.println();
		if (aggressiveFlush) writer.flush();
	}

	@Override
	public void close() throws IOException {
		super.close();
		writer.flush();
	}
}
