package com.github.knokko.bitser.io;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.CollectionField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.serialize.Bitser;

import java.io.*;
import java.nio.file.Files;
import java.util.Random;

public class ArrayBenchmark {

	public static void main(String[] args) throws IOException {
		SlowBooleanArray slowBooleans = new SlowBooleanArray(1234);
		FastBooleanArray fastBooleans = new FastBooleanArray(1234);
		SlowByteArray slowBytes = new SlowByteArray(1234);
		FastByteArray fastBytes = new FastByteArray(1234);
		SlowIntArray slowInts = new SlowIntArray(1234);
		FastIntArray fastInts = new FastIntArray(1234);

		measure("slow booleans", output -> new Bitser(false).serialize(slowBooleans, output));
		measure("fast booleans", output -> new Bitser(false).serialize(fastBooleans, output));
		measure("slow bytes", output -> new Bitser(false).serialize(slowBytes, output));
		measure("fast bytes", output -> new Bitser(false).serialize(fastBytes, output));
		measure("output stream bytes", output -> output.write(slowBytes.data));

		File intsFile = new File("ints.bin");
		DataOutputStream intOutput = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(intsFile.toPath())));
		long startTime = System.nanoTime();
		for (int value : slowInts.data) intOutput.writeInt(value);
		intOutput.flush();
		intOutput.close();
		long endTime = System.nanoTime();
		intsFile.deleteOnExit();

		System.out.println("Took " + (endTime - startTime) / 1000_000 + " ms for data output ints");

		measure("slow ints", output -> new Bitser(false).serialize(slowInts, output));
		measure("fast ints", output -> new Bitser(false).serialize(fastInts, output));
	}

	@FunctionalInterface
	private interface MeasureFunction {

		void write(BitOutputStream output) throws IOException;
	}

	private static void measure(String description, MeasureFunction writeData) throws IOException {

		File target = new File(description + ".bin");
		OutputStream byteOutput = new BufferedOutputStream(Files.newOutputStream(target.toPath()));
		BitOutputStream bitOutput = new BitOutputStream(byteOutput);

		long startTime = System.nanoTime();
		writeData.write(bitOutput);
		bitOutput.finish();
		long endTime = System.nanoTime();

		System.out.println("Took " + (endTime - startTime) / 1000_000 + " ms for " + description);
		target.deleteOnExit();
	}

	@BitStruct(backwardCompatible = false)
	private static class SlowBooleanArray {

		@BitField(ordering = 0)
		@CollectionField
		final boolean[] data = new boolean[80_000_000];

		@SuppressWarnings("unused")
		SlowBooleanArray() {}

		SlowBooleanArray(long seed) {
			Random rng = new Random(seed);
			for (int index = 0; index < data.length; index++) data[index] = rng.nextBoolean();
		}
	}

	@BitStruct(backwardCompatible = false)
	private static class FastBooleanArray {

		@BitField(ordering = 0)
		@CollectionField(writeAsBytes = true)
		final boolean[] data = new boolean[80_000_000];

		@SuppressWarnings("unused")
		FastBooleanArray() {}

		FastBooleanArray(long seed) {
			Random rng = new Random(seed);
			for (int index = 0; index < data.length; index++) data[index] = rng.nextBoolean();
		}
	}

	@BitStruct(backwardCompatible = false)
	private static class SlowByteArray {

		@BitField(ordering = 0)
		@IntegerField(expectUniform = true)
		@CollectionField(size = @IntegerField(minValue = 10_000_000, maxValue = 10_000_000, expectUniform = true))
		final byte[] data = new byte[10_000_000];

		@SuppressWarnings("unused")
		SlowByteArray() {}

		SlowByteArray(long seed) {
			new Random(seed).nextBytes(data);
		}
	}

	@BitStruct(backwardCompatible = false)
	private static class FastByteArray {

		@BitField(ordering = 0)
		@CollectionField(writeAsBytes = true)
		final byte[] data = new byte[10_000_000];

		@SuppressWarnings("unused")
		FastByteArray() {}

		FastByteArray(long seed) {
			new Random(seed).nextBytes(data);
		}
	}

	@BitStruct(backwardCompatible = false)
	private static class SlowIntArray {

		@BitField(ordering = 0)
		@IntegerField(expectUniform = true)
		@CollectionField(size = @IntegerField(minValue = 2_500_000, maxValue = 2_500_000, expectUniform = true))
		final int[] data = new int[2_500_000];

		@SuppressWarnings("unused")
		SlowIntArray() {}

		SlowIntArray(long seed) {
			Random rng = new Random(seed);
			for (int index = 0; index < data.length; index++) data[index] = rng.nextInt();
		}
	}

	@BitStruct(backwardCompatible = false)
	private static class FastIntArray {

		@BitField(ordering = 0)
		@CollectionField(writeAsBytes = true)
		final int[] data = new int[2_500_000];

		@SuppressWarnings("unused")
		FastIntArray() {}

		FastIntArray(long seed) {
			Random rng = new Random(seed);
			for (int index = 0; index < data.length; index++) data[index] = rng.nextInt();
		}
	}
}
