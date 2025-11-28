package com.github.knokko.bitser.util;

public class JobOutput<T> {

	boolean completed;
	T output;

	public T get() {
		if (!completed) throw new IllegalStateException("Attempted to use result too early");
		return output;
	}
}
