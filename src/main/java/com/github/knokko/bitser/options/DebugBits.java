package com.github.knokko.bitser.options;

import java.io.PrintWriter;

public class DebugBits {

	public final PrintWriter writer;
	public final boolean flushAggressively;

	public DebugBits(PrintWriter writer, boolean flushAggressively) {
		this.writer = writer;
		this.flushAggressively = flushAggressively;
	}
}
