package com.github.knokko.bitser.wrapper;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestHelper {

	public static void assertContains(String fullString, String substring) {
		assertTrue(fullString.contains(substring), "Expected '" + fullString + "' to contain '" + substring + "'");
	}
}
