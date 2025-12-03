package com.github.knokko.bitser.io;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.FloatField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.field.NestedFieldSetting;
import com.github.knokko.bitser.Bitser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class DebugStreamPlayground {

	@BitStruct(backwardCompatible = true)
	private static class MiniStruct {

		@BitField(id = 0)
		@FloatField(expectMultipleOf = 0.5)
		float weight = 1.5f;
	}

	@BitStruct(backwardCompatible = true)
	private static class ExampleStruct {

		@BitField(id = 1)
		@IntegerField(expectUniform = false)
		int x = 12;

		@BitField(id = 2)
		String hello = "world";

		@BitField(id = 4)
		@IntegerField(expectUniform = true, minValue = 0, maxValue = 100)
		int percentage = 70;

		@BitField(id = 5)
		boolean cool = true;

		@BitField(id = 6)
		MiniStruct mini = new MiniStruct();

		@BitField(id = 7)
		@NestedFieldSetting(path = "", writeAsBytes = true)
		short[] heights = { 100, 1700 };
	}

	public static void main(String[] args) throws IOException {
		Bitser bitser = new Bitser(false);
		BitOutputStream output = new BitDebugStream(new ByteArrayOutputStream(), new PrintWriter("backward.yml"));
		bitser.serialize(new ExampleStruct(), output, Bitser.BACKWARD_COMPATIBLE);
		output.finish();

		output = new BitDebugStream(new ByteArrayOutputStream(), new PrintWriter("test.yml"));
		bitser.serialize(new ExampleStruct(), output);
		output.finish();
	}
}
