package com.github.knokko.bitser.exceptions;

public class RecursionException extends RuntimeException {

	public final String debugInfoStack;

	public RecursionException(String debugInfoStack, Throwable cause) {
		super("Error during recursion: \"" + cause.getMessage() + "\", debug stack: " + debugInfoStack, cause);
		this.debugInfoStack = debugInfoStack;
	}
}
