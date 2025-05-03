package com.github.knokko.bitser.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class BitDebugStream extends BitOutputStream {

	private final PrintWriter debug;
	private final List<String> stack = new ArrayList<>();
	private boolean wroteLastContext = true;

	public BitDebugStream(OutputStream byteStream, PrintWriter debug) {
		super(byteStream);
		this.debug = debug;
	}

	private void useLastContext() {
		if (!wroteLastContext) {
			for (int counter = 0; counter < stack.size() - 1; counter++) debug.print("  ");
			debug.print(stack.get(stack.size() - 1));
			debug.println(':');
			wroteLastContext = true;
		}
	}

	private void indent() {
		useLastContext();
		for (int counter = 0; counter < stack.size(); counter++) debug.print("  ");
	}

	@Override
	public void write(boolean value) throws IOException {
		super.write(value);
		debug.print(value ? '1' : '0');
	}

	@Override
	public void write(byte[] values) throws IOException {
		super.write(values);
		for (int index = 0; index < values.length - 1; index++) {
			debug.print(values[index]);
			debug.print(',');
		}
		if (values.length > 0) debug.print(values[0]);
	}

	@Override
	public void finish() throws IOException {
		super.finish();
		debug.flush();
		debug.close();
		if (!stack.isEmpty()) throw new IllegalStateException("Ended with stack " + stack);
	}

	@Override
	public void pushContext(String context, int counter) {
		useLastContext();
		wroteLastContext = false;
		if (counter >= 0) stack.add(context + counter);
		else stack.add(context);
	}

	@Override
	public void popContext(String context, int counter) {
		if (stack.isEmpty()) throw new IllegalStateException("Tried to pop " + context + " from empty stack");

		String expected = context;
		if (counter >= 0) expected += counter;
		if (!stack.get(stack.size() - 1).equals(expected)) {
			throw new IllegalArgumentException("Tried to pop " + expected + ", but stack is " + stack);
		}
		stack.remove(stack.size() - 1);
		wroteLastContext = true;
	}

	@Override
	public void prepareProperty(String fieldName, int counter) {
		indent();
		debug.print(fieldName);
		if (counter >= 0) debug.print(counter);
		debug.print(": ");
	}

	@Override
	public void finishProperty() {
		debug.println();
	}
}
