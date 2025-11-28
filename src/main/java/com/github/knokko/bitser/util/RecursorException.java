package com.github.knokko.bitser.util;

public class RecursorException extends RuntimeException {

	public final String debugInfoStack;

	public RecursorException(String debugInfoStack, Throwable cause) {
		super("Error during recursion: \"" + cause.getMessage() + "\", debug stack: " + debugInfoStack, cause);
		this.debugInfoStack = debugInfoStack;
	}
}
