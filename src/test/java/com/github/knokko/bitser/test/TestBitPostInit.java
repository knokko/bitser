package com.github.knokko.bitser.test;

import com.github.knokko.bitser.BitPostInit;
import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.Bitser;
import com.github.knokko.bitser.legacy.BackArrayValue;
import com.github.knokko.bitser.legacy.BackIntValue;
import com.github.knokko.bitser.legacy.BackStringValue;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.FloatField;
import com.github.knokko.bitser.field.FunctionContext;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.options.WithParameter;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.github.knokko.bitser.test.wrapper.TestHelper.assertContains;
import static org.junit.jupiter.api.Assertions.*;

public class TestBitPostInit {

	@BitStruct(backwardCompatible = true)
	private static class DerivedSum implements BitPostInit {

		@BitField(id = 1)
		@IntegerField(expectUniform = false)
		final int a;

		@BitField(id = 0)
		@IntegerField(expectUniform = false)
		final int b;

		int c;

		DerivedSum(int a, int b) {
			this.a = a;
			this.b = b;
			this.c = a + b;
		}

		@SuppressWarnings("unused")
		DerivedSum() {
			this(0, 0);
		}

		@Override
		public void postInit(Context context) {
			this.c = this.a + this.b;
		}
	}

	@Test
	public void testDerivedSum() {
		Bitser bitser = new Bitser(false);
		DerivedSum original = new DerivedSum(3, 4);
		assertEquals(3, original.a);
		assertEquals(7, original.c);

		DerivedSum loaded = bitser.stupidDeepCopy(original);
		assertEquals(3, loaded.a);
		assertEquals(4, loaded.b);
		assertEquals(7, loaded.c);
	}

	@BitStruct(backwardCompatible = true)
	private static class LegacyConversionBefore {

		@SuppressWarnings("unused")
		@BitField(id = 4)
		@IntegerField(expectUniform = true, minValue = 0, maxValue = 100)
		final int percentage = 75;

		@SuppressWarnings("unused")
		@BitField(id = 0)
		final String hello = "world";
	}

	@BitStruct(backwardCompatible = true)
	private static class BeforeParent extends LegacyConversionBefore {

		@SuppressWarnings("unused")
		@BitField(id = 4)
		final String owner = "me";
	}

	@BitStruct(backwardCompatible = true)
	private static class LegacyConversionAfter implements BitPostInit {

		@BitField(id = 0)
		String hello;

		@BitField(id = 1)
		@FloatField(expectMultipleOf = 0.01)
		double fraction;

		@Override
		public void postInit(Context context) {
			assertEquals("world", hello);
			long percentage = ((BackIntValue) context.legacyFieldValues.get(LegacyConversionAfter.class)[4]).value;
			this.fraction = percentage * 0.01;
		}
	}

	@BitStruct(backwardCompatible = true)
	private static class AfterParent extends LegacyConversionAfter {

		@BitField(id = 1)
		String[] owners;

		@Override
		public void postInit(Context context) {
			super.postInit(context);
			this.owners = new String[] { ((BackStringValue) context.legacyFieldValues.get(AfterParent.class)[4]).value };
		}
	}

	@Test
	public void testLegacyConversion() {
		Bitser bitser = new Bitser(false);
		assertEquals(0.75, bitser.deserializeFromBytesSimple(
				LegacyConversionAfter.class,
				bitser.serializeToBytesSimple(new LegacyConversionBefore(), Bitser.BACKWARD_COMPATIBLE),
				Bitser.BACKWARD_COMPATIBLE).fraction
		);
	}

	@Test
	public void testLegacyConversionWithInheritance() {
		Bitser bitser = new Bitser(true);
		AfterParent parent = bitser.deserializeFromBytesSimple(
				AfterParent.class, bitser.serializeToBytesSimple(new BeforeParent(), Bitser.BACKWARD_COMPATIBLE),
				Bitser.BACKWARD_COMPATIBLE
		);
		assertArrayEquals(new String[] { "me" }, parent.owners);
		assertEquals(0.75, parent.fraction);
	}

	@BitStruct(backwardCompatible = true)
	private static class DefaultValue implements BitPostInit {

		@BitField(id = 0)
		String value = "";

		@Override
		public void postInit(Context context) {
			if (value.isEmpty()) value = (String) context.withParameters.get("default-value");
		}
	}

	@Test
	public void testWithParameters() {
		Bitser bitser = new Bitser(true);
		DefaultValue customValue = new DefaultValue();
		customValue.value = "hello";

		DefaultValue defaultValue = bitser.stupidDeepCopy(
				new DefaultValue(), new WithParameter("default-value", "it worked"), new WithParameter("unused", 1234)
		);
		customValue = bitser.stupidDeepCopy(customValue);
		assertEquals("it worked", defaultValue.value);
		assertEquals("hello", customValue.value);

		defaultValue = bitser.deserializeFromBytesSimple(DefaultValue.class, bitser.serializeToBytesSimple(
				new DefaultValue(), Bitser.BACKWARD_COMPATIBLE
		), Bitser.BACKWARD_COMPATIBLE, new WithParameter("default-value", "also backward-compatible"));
		assertEquals("also backward-compatible", defaultValue.value);
	}

	@Test
	public void testDuplicateDeserializeWithParameter() {
		String errorMessage = assertThrows(
				IllegalArgumentException.class,
				() -> new Bitser(false).deserializeFromBytesSimple(
					AfterParent.class, new byte[0], new WithParameter("ok", 1), new WithParameter("ok", 2))
		).getMessage();
		assertContains(errorMessage, "Duplicate with parameter");
		assertContains(errorMessage, "ok");
	}

	@Test
	public void testDuplicateSerializeWithParameter() {
		String errorMessage = assertThrows(
				IllegalArgumentException.class,
				() -> new Bitser(false).serializeToBytesSimple(
					new AfterParent(), new WithParameter("ok", 1), new WithParameter("ok", 2))
		).getMessage();
		assertContains(errorMessage, "Duplicate with parameter");
		assertContains(errorMessage, "ok");
	}

	@BitStruct(backwardCompatible = true)
	private static class SaveClassName implements BitPostInit {

		private Class<?> wrapped;

		@BitField(id = 1)
		@IntegerField(expectUniform = false)
		int x;

		@SuppressWarnings("unused")
		@BitField(id = 1)
		private String className() {
			return wrapped.getName();
		}

		@Override
		public void postInit(Context context) {
			assertEquals(1234, x);
			try {
				this.wrapped = Class.forName((String) context.functionValues.get(SaveClassName.class)[1]);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Test
	public void testReadFunctionValues() {
		SaveClassName original = new SaveClassName();
		original.wrapped = String.class;
		original.x = 1234;

		SaveClassName loaded = new Bitser(true).stupidDeepCopy(original);
		assertSame(String.class, loaded.wrapped);
		assertEquals(1234, loaded.x);
	}

	@BitStruct(backwardCompatible = true)
	private static class MultipleClassNames implements BitPostInit {

		private final List<Class<?>> classes = new ArrayList<>();

		@BitField(id = 1)
		@IntegerField(expectUniform = true)
		int x;

		@SuppressWarnings("unused")
		@BitField(id = 2)
		private ArrayList<String> classNames() {
			ArrayList<String> names = new ArrayList<>();
			for (Class<?> wrapped : classes) names.add(wrapped.getName());
			return names;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void postInit(Context context) {
			List<String> names;
			assertEquals(-12, x);
			if (context.legacyFunctionValues == null) {
				names = (List<String>) context.functionValues.get(MultipleClassNames.class)[2];
			} else {
				Object[] values = context.legacyFunctionValues.get(MultipleClassNames.class);
				Object[] currentValues = context.functionValues.get(MultipleClassNames.class);
				assertEquals(-12L, ((BackIntValue) context.legacyFieldValues.get(MultipleClassNames.class)[1]).value);
				if (values[1] != null) {
					names = new ArrayList<>(1);
					names.add(((BackStringValue) values[1]).value);
					assertArrayEquals(new Object[3], currentValues);
				} else {
					BackArrayValue legacyNames = (BackArrayValue) values[2];
					Object[] rawNames = (Object[]) legacyNames.array;
					names = new ArrayList<>(rawNames.length);
					for (Object rawName : rawNames) names.add(((BackStringValue) rawName).value);

					List<String> expectedListAtIndex2 = new ArrayList<>(2);
					expectedListAtIndex2.add("java.io.File");
					expectedListAtIndex2.add("java.nio.file.Path");
					assertArrayEquals(new Object[] { null, null, expectedListAtIndex2}, currentValues);
				}
			}

			for (String name : names) {
				try {
					classes.add(Class.forName(name));
				} catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	@Test
	public void testSingleToMultipleClassNames() {
		Bitser bitser = new Bitser(false);

		SaveClassName original = new SaveClassName();
		original.wrapped = IOException.class;
		original.x = -12;

		MultipleClassNames multiple = bitser.deserializeFromBytesSimple(
				MultipleClassNames.class,
				bitser.serializeToBytesSimple(original, Bitser.BACKWARD_COMPATIBLE),
				Bitser.BACKWARD_COMPATIBLE
		);
		assertEquals(-12, multiple.x);
		assertEquals(1, multiple.classes.size());
		assertSame(IOException.class, multiple.classes.get(0));
	}

	@Test
	public void testMultipleClasses() {
		Bitser bitser = new Bitser(true);
		MultipleClassNames original = new MultipleClassNames();
		original.x = -12;
		original.classes.add(Integer.class);
		original.classes.add(Class.class);

		MultipleClassNames loaded = bitser.stupidDeepCopy(original);
		assertEquals(-12, loaded.x);
		assertEquals(original.classes, loaded.classes);
	}

	@Test
	public void testMultipleClassesBackwardCompatible() {
		Bitser bitser = new Bitser(false);
		MultipleClassNames original = new MultipleClassNames();
		original.x = -12;
		original.classes.add(File.class);
		original.classes.add(Path.class);

		MultipleClassNames loaded = bitser.deserializeFromBytesSimple(
				MultipleClassNames.class, bitser.serializeToBytesSimple(original, Bitser.BACKWARD_COMPATIBLE),
				Bitser.BACKWARD_COMPATIBLE
		);
		assertEquals(-12, loaded.x);
		assertEquals(original.classes, loaded.classes);
	}

	@BitStruct(backwardCompatible = true)
	private static class ClassSet implements BitPostInit {

		private final Set<Class<?>> classes = new HashSet<>();

		@SuppressWarnings("unused")
		@BitField(id = 2)
		private HashSet<String> classNames() {
			HashSet<String> names = new HashSet<>();
			for (Class<?> wrapped : classes) names.add(wrapped.getName());
			return names;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void postInit(BitPostInit.Context context) {
			Object[] legacyValues = context.legacyFunctionValues.get(ClassSet.class);
			Object[] currentValues = context.functionValues.get(ClassSet.class);

			Set<String> names = (Set<String>) currentValues[2];
			assertArrayEquals(new Object[] { null, null, names }, currentValues);

			BackArrayValue legacyNamesInstance = (BackArrayValue) legacyValues[2];
			assertArrayEquals(new Object[] { null, null, legacyNamesInstance }, legacyValues);
			for (String name : names) {
				try {
					classes.add(Class.forName(name));
				} catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	@Test
	public void testAutomaticConversion() {
		Bitser bitser = new Bitser(false);
		MultipleClassNames original = new MultipleClassNames();
		original.classes.add(File.class);
		original.classes.add(Path.class);

		ClassSet loaded = bitser.deserializeFromBytesSimple(
				ClassSet.class,
				bitser.serializeToBytesSimple(original, Bitser.BACKWARD_COMPATIBLE),
				Bitser.BACKWARD_COMPATIBLE
		);
		assertEquals(2, loaded.classes.size());
		assertTrue(loaded.classes.contains(File.class));
		assertTrue(loaded.classes.contains(Path.class));
	}

	@BitStruct(backwardCompatible = true)
	private static class UsesFunctionContext implements BitPostInit {

		int c;

		@SuppressWarnings("unused")
		@BitField(id = 2)
		DerivedSum create(FunctionContext context) {
			return new DerivedSum(5, (int) context.withParameters.get("b"));
		}

		@Override
		public void postInit(Context context) {
			this.c = ((DerivedSum) context.functionValues.get(UsesFunctionContext.class)[2]).c;
		}
	}

	@Test
	public void testBackwardCompatibleFunctionContext() {
		Bitser bitser = new Bitser(false);
		UsesFunctionContext loaded = bitser.deserializeFromBytesSimple(
				UsesFunctionContext.class,
				bitser.serializeToBytesSimple(
						new UsesFunctionContext(), Bitser.BACKWARD_COMPATIBLE, new WithParameter("b", 7)
				),
				Bitser.BACKWARD_COMPATIBLE
		);
		assertEquals(12, loaded.c);

		// TODO Also test without backward compatibility
	}
}
