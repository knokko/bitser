package com.github.knokko.bitser.io;

import com.github.knokko.bitser.RecursionNode;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Arrays;

public class DebugBitOutputStream extends BitOutputStream {

	private final PrintWriter writer;
	private final boolean aggressiveFlush;

	public DebugBitOutputStream(OutputStream byteStream, PrintWriter writer, boolean aggressiveFlush) {
		super(byteStream);
		this.writer = writer;
		this.aggressiveFlush = aggressiveFlush;
	}

	@Override
	public void write(boolean bit) throws IOException {
		super.write(bit);
		writer.print(bit ? '1' : '0');
		if (aggressiveFlush) writer.flush();
	}

	@Override
	public void write(int value, int numBits) throws IOException {
		super.write(value, numBits);
		for (int bit = 0; bit < numBits; bit++) writer.print((value & (1 << bit)) != 0 ? '1' : '0');
		if (aggressiveFlush) writer.flush();
	}

	@Override
	public void write(byte[] bytes) throws IOException {
		super.write(bytes);
		writer.print(Arrays.toString(bytes));
		if (aggressiveFlush) writer.flush();
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
	public void finish() throws IOException {
		super.finish();
		writer.println("---------------------------------------------------------------------------------------------");
		writer.flush();
	}
}
