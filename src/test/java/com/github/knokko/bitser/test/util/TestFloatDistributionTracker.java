package com.github.knokko.bitser.test.util;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.Bitser;
import com.github.knokko.bitser.field.FloatField;
import com.github.knokko.bitser.util.FloatDistributionTracker;

import java.io.PrintWriter;

public class TestFloatDistributionTracker {

	@BitStruct(backwardCompatible = false)
	private static class ExampleStruct {

		@FloatField
		float[] numbers;

		@FloatField
		double lonely;
	}

	public static void main(String[] args) {
		Bitser bitser = new Bitser(false);
		ExampleStruct example = new ExampleStruct();
		example.numbers = new float[] { 3.1f, 5.5f, 5.5f, 5.5f, 1.2f, 2.8f, 3.1f, 2.7f, 5.5f };
		example.lonely = -12.5;

		FloatDistributionTracker distribution = new FloatDistributionTracker();
		bitser.serializeToBytes(example, distribution);

		distribution.printFieldOccurrences(new PrintWriter(System.out));
		System.out.println();
		distribution.printFieldValueOccurrences(new PrintWriter(System.out), 100, 100);
		System.out.println();
		distribution.optimize(
				new PrintWriter(System.out), 3, 1,
				2, new double[] { 0.0, 0.1, 0.5 }
		);
	}
}
