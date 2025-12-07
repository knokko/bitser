package com.github.knokko.bitser.test.util;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.Bitser;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.util.IntegerDistributionTracker;

import java.io.PrintWriter;

public class TestIntegerDistributionTracker {

	@BitStruct(backwardCompatible = false)
	private static class ExampleStruct {

		@IntegerField(expectUniform = false)
		int[] numbers;

		@IntegerField(expectUniform = false)
		long lonely;
	}

	public static void main(String[] args) {
		Bitser bitser = new Bitser(false);
		ExampleStruct example = new ExampleStruct();
		example.numbers = new int[] { 3, 5, 5, 5, 1, 2, 3, 2, 5 };
		example.lonely = -12;

		IntegerDistributionTracker distribution = new IntegerDistributionTracker();
		bitser.serializeToBytes(example, distribution);

		distribution.printFieldOccurrences(new PrintWriter(System.out));
		System.out.println();
		distribution.printFieldValueOccurrences(new PrintWriter(System.out), 100, 100);
		System.out.println();
		distribution.optimize(new PrintWriter(System.out), 3, 1, 2);
	}
}
