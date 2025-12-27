package com.github.knokko.bitser.options;

import java.io.PrintWriter;

public record DebugBits(PrintWriter writer, boolean flushAggressively) {

	public static final DebugBits HARD = new DebugBits(new PrintWriter(System.out), true);
}
