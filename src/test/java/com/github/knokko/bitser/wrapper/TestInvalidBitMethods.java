package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.FunctionContext;
import com.github.knokko.bitser.serialize.Bitser;
import com.github.knokko.bitser.util.RecursorException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

import static com.github.knokko.bitser.wrapper.TestHelper.assertContains;
import static org.junit.jupiter.api.Assertions.*;

public class TestInvalidBitMethods {

	@BitStruct(backwardCompatible = false)
	private static class StaticBitMethod {

		@SuppressWarnings("unused")
		@BitField(id = 0)
		private static String test() {
			return "hello";
		}
	}

	@Test
	public void testForbidStaticBitMethods() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class, () -> new Bitser(false).serializeToBytes(new StaticBitMethod())
		).getMessage();
		assertContains(errorMessage, "must not be static");
		assertContains(errorMessage, "StaticBitMethod.test");
	}

	@BitStruct(backwardCompatible = true)
	private static class MultipleParameters {

		@SuppressWarnings("unused")
		@BitField(id = 1)
		private String test(int x, int y) {
			return x + "," + y;
		}
	}

	@Test
	public void testForbidMultipleParameters() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class, () -> new Bitser(false).serializeToBytes(new MultipleParameters())
		).getMessage();
		assertContains(errorMessage, "at most 1 parameter");
		assertContains(errorMessage, "MultipleParameters.test");
	}

	@BitStruct(backwardCompatible = false)
	private static class WrongParameter {

		@SuppressWarnings("unused")
		@BitField(id = 0)
		private String wrong(String hello) {
			return hello;
		}
	}

	@Test
	public void testForbidWrongParameterType() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class, () -> new Bitser(false).serializeToBytes(new WrongParameter())
		).getMessage();
		assertContains(errorMessage, "parameter type must be FunctionContext");
		assertContains(errorMessage, "WrongParameter.wrong");
	}

	@BitStruct(backwardCompatible = true)
	private static class WithoutID {

		@SuppressWarnings("unused")
		@BitField
		private String missing() {
			return "missing";
		}
	}

	@Test
	public void testIDsAreRequired() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class, () -> new Bitser(false).serializeToBytes(new WithoutID())
		).getMessage();
		assertContains(errorMessage, "method IDs must be non-negative");
		assertContains(errorMessage, "WithoutID.missing");
	}

	@BitStruct(backwardCompatible = true)
	private static class DuplicateID {

		@SuppressWarnings("unused")
		@BitField(id = 1)
		private String hello() {
			return "hello";
		}

		@SuppressWarnings("unused")
		@BitField(id = 1)
		private String world() {
			return "world";
		}
	}

	@Test
	public void testForbidDuplicateIDs() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class, () -> new Bitser(false).serializeToBytes(new DuplicateID())
		).getMessage();
		assertContains(errorMessage, "multiple @BitField methods with id 1");
		assertContains(errorMessage, "DuplicateID");
	}

	@BitStruct(backwardCompatible = false)
	private static class ThrowsError {

		@SuppressWarnings("unused")
		@BitField(id = 0)
		private String throwsError() {
			throw new Error("nothing personal");
		}
	}

	@Test
	public void testPropagateErrors() {
		RecursorException exception = assertThrows(
				RecursorException.class, () -> new Bitser(false).serializeToBytes(new ThrowsError())
		);
		assertEquals("nothing personal", exception.getCause().getMessage());
		assertContains(exception.debugInfoStack, "-> throwsError");
	}

	@BitStruct(backwardCompatible = false)
	private static class ThrowsUncheckedException {

		@SuppressWarnings("unused")
		@BitField(id = 0)
		private String throwsError() {
			throw new UnsupportedOperationException();
		}
	}

	@Test
	public void testPropagateUncheckedExceptions() {
		RecursorException exception = assertThrows(
				RecursorException.class,
				() -> new Bitser(false).serializeToBytes(new ThrowsUncheckedException())
		);
		assertInstanceOf(UnsupportedOperationException.class, exception.getCause());
		assertContains(exception.debugInfoStack, "-> throwsError");
	}

	@BitStruct(backwardCompatible = true)
	private static class ThrowsCheckedException {

		@SuppressWarnings("unused")
		@BitField(id = 5)
		private UUID throwsIO(FunctionContext context) throws IOException {
			throw new IOException("nope: " + context);
		}
	}

	@Test
	public void testPropagateCheckedExceptions() {
		RecursorException exception = assertThrows(
				RecursorException.class,
				() -> new Bitser(true).serializeToBytes(new ThrowsCheckedException())
		);
		IOException cause = (IOException) exception.getCause();
		assertContains(cause.getMessage(), "nope: ");
	}
}
