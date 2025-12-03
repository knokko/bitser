package com.github.knokko.bitser.test.util;

import com.github.knokko.bitser.util.JobOutput;
import com.github.knokko.bitser.util.Recursor;
import com.github.knokko.bitser.util.RecursorException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestRecursor {

	private static final Object DUMMY_INFO = new Object();

	private static class StringSpitter {

		final String flatValue;
		final List<StringSpitter> children = new ArrayList<>();

		RuntimeException flatException;

		Error localError, nestedError;

		StringSpitter(String flatValue) {
			this.flatValue = flatValue;
		}

		void recursorSpit(Recursor<List<String>, Object> recursor) {
			assertSame(DUMMY_INFO, recursor.info);
			if (flatValue != null) recursor.runFlat("flatValue", context -> context.add(flatValue));
			if (flatException != null) recursor.runFlat("flatException", context -> {
				throw flatException;
			});
			for (StringSpitter child : children) recursor.runNested(flatValue, child::recursorSpit);
			if (nestedError != null) recursor.runNested("nestedError", nested -> {
				throw nestedError;
			});
			if (localError != null) throw localError;
		}
	}

	private static class ComputeSpitter {

		final String flatValue;
		final List<ComputeSpitter> children = new ArrayList<>();

		ComputeSpitter(String flatValue) {
			this.flatValue = flatValue;
		}

		private long product(List<JobOutput<Integer>> outputs) {
			long product = 1L;
			for (JobOutput<Integer> output : outputs) product *= output.get();
			return product;
		}

		int recursorSpit(Recursor<List<String>, Object> recursor, long suffix) {
			assertSame(DUMMY_INFO, recursor.info);
			List<JobOutput<Integer>> previousOutputs = new ArrayList<>();
			if (flatValue != null) {
				previousOutputs.add(recursor.computeFlat("flatValue", context -> {
					context.add(flatValue + suffix);
					return context.size();
				}));
			}
			for (ComputeSpitter child : children) {
				List<JobOutput<Integer>> outputSoFar = new ArrayList<>(previousOutputs);
				previousOutputs.add(recursor.computeNested(flatValue, nested ->
						child.recursorSpit(nested, suffix * product(outputSoFar)))
				);
			}
			return 1 + previousOutputs.size();
		}
	}

	@Test
	public void testStringSpitting() {
		StringSpitter spitterB = new StringSpitter(null);
		spitterB.children.add(new StringSpitter("root.b1"));
		spitterB.children.add(new StringSpitter("root.b3"));

		StringSpitter spitterD = new StringSpitter("root.d");
		spitterD.children.add(new StringSpitter("root.d-only"));

		StringSpitter rootSpitter = new StringSpitter("root");
		rootSpitter.children.add(new StringSpitter("root.a"));
		rootSpitter.children.add(spitterB);
		rootSpitter.children.add(new StringSpitter("root.c"));
		rootSpitter.children.add(spitterD);
		rootSpitter.children.add(new StringSpitter("root.e"));

		List<String> expectedLines = new ArrayList<>();
		expectedLines.add("root");
		expectedLines.add("root.a");
		expectedLines.add("root.b1");
		expectedLines.add("root.b3");
		expectedLines.add("root.c");
		expectedLines.add("root.d");
		expectedLines.add("root.d-only");
		expectedLines.add("root.e");

		List<String> actualLines = new ArrayList<>();
		Recursor.run(actualLines, DUMMY_INFO, rootSpitter::recursorSpit);

		assertEquals(expectedLines, actualLines);
	}

	@Test
	public void testComputeSpitting() {
		ComputeSpitter spitterB = new ComputeSpitter(null);
		spitterB.children.add(new ComputeSpitter("root.b1:"));
		spitterB.children.add(new ComputeSpitter("root.b3:"));

		ComputeSpitter spitterD = new ComputeSpitter("root.d:");
		spitterD.children.add(new ComputeSpitter("root.d-only:"));

		ComputeSpitter rootSpitter = new ComputeSpitter("root:");
		rootSpitter.children.add(new ComputeSpitter("root.a:"));
		rootSpitter.children.add(spitterB);
		rootSpitter.children.add(new ComputeSpitter("root.c:"));
		rootSpitter.children.add(spitterD);
		rootSpitter.children.add(new ComputeSpitter("root.e:"));

		List<String> expectedLines = new ArrayList<>();
		expectedLines.add("root:1");
		expectedLines.add("root.a:1");
		expectedLines.add("root.b1:2");
		expectedLines.add("root.b3:4");
		expectedLines.add("root.c:6");
		expectedLines.add("root.d:12");
		expectedLines.add("root.d-only:72");
		expectedLines.add("root.e:36");

		List<String> actualLines = new ArrayList<>();
		int result = Recursor.compute(actualLines, DUMMY_INFO, recursor ->
				rootSpitter.recursorSpit(recursor, 1)
		);

		assertEquals(expectedLines, actualLines);
		assertEquals(7, result);
	}

	@Test
	public void testExceptionHandling() {
		StringSpitter fangs = new StringSpitter("fangs");

		StringSpitter eli = new StringSpitter("eli");
		eli.children.add(fangs);

		StringSpitter darius = new StringSpitter(null);
		darius.children.add(eli);

		StringSpitter dave = new StringSpitter("dave");
		dave.children.add(new StringSpitter("Evelyn"));

		StringSpitter cali = new StringSpitter("Cali");
		cali.children.add(darius);
		cali.children.add(dave);

		StringSpitter bert = new StringSpitter("Bert");
		bert.children.add(cali);

		StringSpitter root = new StringSpitter("Ali");
		root.children.add(bert);

		// Test without exception
		Recursor.run(new ArrayList<>(), DUMMY_INFO, root::recursorSpit);

		// Test flat exception
		fangs.flatException = new IllegalStateException("Nah");
		RecursorException flatException = assertThrows(
				RecursorException.class,
				() -> Recursor.run(new ArrayList<>(), DUMMY_INFO, root::recursorSpit)
		);
		assertSame(fangs.flatException, flatException.getCause());
		assertEquals(
				"Error during recursion: \"Nah\", debug stack: " +
						"Recursor Root -> Ali -> Bert -> Cali -> null -> eli -> flatException",
				flatException.getMessage()
		);

		// Test nested exception
		fangs.flatException = null;
		fangs.nestedError = new AbstractMethodError("Fake IO");
		RecursorException nestedError = assertThrows(
				RecursorException.class,
				() -> Recursor.run(new ArrayList<>(), DUMMY_INFO, root::recursorSpit)
		);
		assertSame(fangs.nestedError, nestedError.getCause());
		assertEquals(
				"Error during recursion: \"Fake IO\", debug stack: " +
						"Recursor Root -> Ali -> Bert -> Cali -> null -> eli -> nestedError",
				nestedError.getMessage()
		);

		// Test local exceptions
		fangs.nestedError = null;
		fangs.localError = new Error("Cache locality :P");
		RecursorException localError = assertThrows(
				RecursorException.class,
				() -> Recursor.run(new ArrayList<>(), DUMMY_INFO, root::recursorSpit)
		);
		assertSame(fangs.localError, localError.getCause());
		assertEquals(
				"Error during recursion: \"Cache locality :P\", debug stack: " +
						"Recursor Root -> Ali -> Bert -> Cali -> null -> eli",
				localError.getMessage()
		);
	}
}
