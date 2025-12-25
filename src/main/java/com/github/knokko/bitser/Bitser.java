package com.github.knokko.bitser;

import com.github.knokko.bitser.io.*;
import com.github.knokko.bitser.exceptions.*;
import com.github.knokko.bitser.field.FunctionContext;
import com.github.knokko.bitser.options.CollectionSizeLimit;
import com.github.knokko.bitser.options.DebugBits;
import com.github.knokko.bitser.options.WithParameter;
import com.github.knokko.bitser.util.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

public class Bitser {

	public static final Object BACKWARD_COMPATIBLE = new Object();
	public static final Object FORBID_LAZY_SAVING = new Object();

	final BitserCache cache;

	public Bitser(boolean threadSafe) {
		this.cache = new BitserCache(threadSafe);
	}

	private void rethrowRecursorException(RecursorException failure) throws IOException {
		Throwable cause = failure.getCause();
		if (cause instanceof InvalidBitValueException) {
			throw new InvalidBitValueException(
					"Invalid value at " + failure.debugInfoStack + ": " + cause.getMessage()
			);
		}
		if (cause instanceof InvalidBitFieldException) {
			throw new InvalidBitFieldException(
					"Invalid field at " + failure.debugInfoStack + ": " + cause.getMessage()
			);
		}
		if (cause instanceof LegacyBitserException) {
			throw new LegacyBitserException(
					"Legacy problem at " + failure.debugInfoStack + ": " + cause.getMessage()
			);
		}
		if (cause instanceof ReferenceBitserException) {
			throw new ReferenceBitserException(
					"Reference problem at " + failure.debugInfoStack + ": " + cause.getMessage()
			);
		}
		if (cause instanceof IOException) {
			throw new IOException(
					"IO exception at " + failure.debugInfoStack + ": " + cause.getMessage(), cause
			);
		}
	}

	public void serializeSimple(
			Object object, BitOutputStream output, Object... withAndOptions
	) throws IOException, BitserException, RecursorException, IllegalArgumentException {
		try {
			rawSerializeSimple(object, output, withAndOptions);
		} catch (RecursorException failure) {
			rethrowRecursorException(failure);
			throw failure;
		}
	}

	private void rawSerializeSimple(Object object, BitOutputStream output, Object... withAndOptions) {
		BitStructWrapper<?> wrapper = cache.getWrapper(object.getClass());

		boolean backwardCompatible = false;
		boolean forbidLazySaving = false;
		IntegerDistributionTracker integerDistribution = null;
		FloatDistributionTracker floatDistribution = null;
		Map<String, Object> withParameters = new HashMap<>();

		for (Object withObject : withAndOptions) {
			if (withObject == BACKWARD_COMPATIBLE) backwardCompatible = true;
			if (withObject == FORBID_LAZY_SAVING) forbidLazySaving = true;
			if (withObject instanceof IntegerDistributionTracker) {
				integerDistribution = (IntegerDistributionTracker) withObject;
			}
			if (withObject instanceof FloatDistributionTracker) {
				floatDistribution = (FloatDistributionTracker) withObject;
			}
			if (withObject instanceof WithParameter) {
				WithParameter parameter = (WithParameter) withObject;
				if (withParameters.containsKey(parameter.key)) {
					throw new IllegalArgumentException("Duplicate with parameter " + parameter.key);
				}
				withParameters.put(parameter.key, parameter.value);
			}
		}

		if (backwardCompatible) {
			LegacyClasses legacy = new LegacyClasses();

			// TODO Eliminate
			legacy.setRoot(Recursor.compute(
					legacy, new LegacyInfo(cache, new FunctionContext(this, true, withParameters)),
					recursor -> wrapper.registerClasses(object, recursor)
			).get());

			try {
				output.pushContext(new RecursionNode("<<<<<<<<<<<<<<<<<<<< LEGACY >>>>>>>>>>>>>>>>>"), null);
				serializeSimple(legacy, output, new WithParameter("legacy-classes", legacy));
				output.popContext(new RecursionNode("<<<<<<<<<<<<<<<<<<<< LEGACY >>>>>>>>>>>>>>>>>"), null);
			} catch (IOException io) {
				throw new RuntimeException(io);
			}
		}

		ArrayList<Object> withObjects = new ArrayList<>();
		for (Object maybeWithObject : withAndOptions) {
			if (maybeWithObject == BACKWARD_COMPATIBLE || maybeWithObject == FORBID_LAZY_SAVING) continue;
			if (maybeWithObject instanceof WithParameter || maybeWithObject instanceof DistributionTracker) continue;
			if (maybeWithObject instanceof DebugBits) continue;
			withObjects.add(maybeWithObject);
		}

		Serializer serializer = new Serializer(
				this, withParameters, output, backwardCompatible, object,
				forbidLazySaving, integerDistribution, floatDistribution
		);
		serializer.references.setWithObjects(withObjects);
		serializer.run();
	}

	public byte[] serializeToBytesSimple(Object object, Object... withAndOptions) {
		DebugBits debug = null;
		for (Object option : withAndOptions) {
			if (option instanceof DebugBits) debug = (DebugBits) option;
		}

		try {
			ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
			BitOutputStream bitOutput = debug != null ?
					new DebugBitOutputStream(byteOutput, debug.writer, debug.flushAggressively) :
					new BitOutputStream(byteOutput);
			serializeSimple(object, bitOutput, withAndOptions);
			bitOutput.finish();
			return byteOutput.toByteArray();
		} catch (IOException shouldNotHappen) {
			throw new UnexpectedBitserException("ByteArrayOutputStream threw an IOException?");
		}
	}

	public <T> T deserializeSimple(
			Class<T> objectClass, BitInputStream input, Object... withAndOptions
	) throws IOException, BitserException, RecursorException, IllegalArgumentException {
		try {
			return rawDeserializeSimple(objectClass, input, withAndOptions);
		} catch (RecursorException failure) {
			rethrowRecursorException(failure);
			throw failure;
		}
	}

	private <T> T rawDeserializeSimple(Class<T> objectClass, BitInputStream input, Object... withAndOptions) throws IOException {
		CollectionSizeLimit sizeLimit = null;
		boolean backwardCompatible = false;
		for (Object withObject : withAndOptions) {
			if (withObject == BACKWARD_COMPATIBLE) {
				backwardCompatible = true;
			}
			if (withObject instanceof CollectionSizeLimit) {
				if (sizeLimit != null) throw new IllegalArgumentException("Encountered multiple size limits");
				sizeLimit = (CollectionSizeLimit) withObject;
			}
		}

		LegacyClasses legacy = null;
		if (backwardCompatible) {
			input.pushContext(new RecursionNode("<<<<<<<<<<<<<<<<<<<< LEGACY >>>>>>>>>>>>>>>>>"), null);
			legacy = deserializeSimple(LegacyClasses.class, input, sizeLimit);
			input.popContext(new RecursionNode("<<<<<<<<<<<<<<<<<<<< LEGACY >>>>>>>>>>>>>>>>>"), null);
		}

		BitStructWrapper<T> wrapper = cache.getWrapper(objectClass);

		Map<String, Object> withParameters = new HashMap<>();
		for (Object withObject : withAndOptions) {
			if (withObject instanceof WithParameter) {
				WithParameter parameter = (WithParameter) withObject;
				if (withParameters.containsKey(parameter.key)) {
					throw new IllegalArgumentException("Duplicate with parameter " + parameter.key);
				}
				withParameters.put(parameter.key, parameter.value);
			}
		}

		ArrayList<Object> withObjects = new ArrayList<>();
		for (Object maybeWithObject : withAndOptions) {
			if (maybeWithObject == null || maybeWithObject == BACKWARD_COMPATIBLE) continue;
			if (maybeWithObject == FORBID_LAZY_SAVING) {
				System.out.println("Ignoring FORBID_LAZY_SAVING option in Bitser.deserialize");
				continue;
			}
			if (maybeWithObject instanceof WithParameter || maybeWithObject instanceof CollectionSizeLimit) continue;
			if (maybeWithObject instanceof DebugBits) continue;
			if (maybeWithObject instanceof DistributionTracker) {
				System.out.println("Ignoring DistributionTracker option in Bitser.deserialize");
				continue;
			}

			withObjects.add(maybeWithObject);
		}

		if (legacy != null) {
			BackDeserializer deserializer = new BackDeserializer(
					this, input, legacy, sizeLimit, withParameters, wrapper
			);
			deserializer.references.setWithObjects(withObjects);
			deserializer.run();
			//noinspection unchecked
			return (T) deserializer.rootStruct;
		} else {
			Deserializer deserializer = new Deserializer(this, input, sizeLimit, withParameters, wrapper);
			deserializer.references.setWithObjects(withObjects);
			deserializer.run();
			//noinspection unchecked
			return (T) deserializer.rootStruct;
		}
	}

	public <T> T deserializeFromBytesSimple(
			Class<T> objectClass, byte[] bytes, Object... withAndOptions
	) throws BitserException, RecursorException, IllegalArgumentException {
		DebugBits debug = null;
		for (Object option : withAndOptions) {
			if (option instanceof DebugBits) debug = (DebugBits) option;
		}
		try {
			ByteArrayInputStream byteInput = new ByteArrayInputStream(bytes);
			BitInputStream bitInput = debug != null ?
					new DebugBitInputStream(byteInput, debug.writer, debug.flushAggressively) :
					new BitInputStream(byteInput);
			T result = deserializeSimple(objectClass, bitInput, withAndOptions);
			bitInput.close();
			return result;
		} catch (IOException shouldNotHappen) {
			throw new IllegalArgumentException("bytes is too short or invalid/corrupted", shouldNotHappen);
		}
	}

	public <T> T shallowCopy(T object) {
		//noinspection unchecked
		return (T) cache.getWrapper(object.getClass()).shallowCopy(object);
	}

	public <T> T stupidDeepCopy(
			T object, Object... with
	) throws BitserException, RecursorException, IllegalArgumentException {
		//noinspection unchecked
		return (T) deserializeFromBytesSimple(object.getClass(), serializeToBytesSimple(object, with), with);
	}

//	public <T> BitStructConnection<T> createStructConnection(
//			T initialState, Consumer<BitStructConnection.ChangeListener> reportChanges
//	) {
//		return createRawStructConnection(initialState, initialState.getClass(), reportChanges);
//	}
//
//	private <T> BitStructConnection<T> createRawStructConnection(
//			T initialState, Class<?> theClass, Consumer<BitStructConnection.ChangeListener> reportChanges
//	) {
//		return cache.getWrapper(theClass).createConnection(this, initialState, reportChanges);
//	}
//
//	public boolean needsChildConnection(BitFieldWrapper childWrapper) {
//		if (childWrapper instanceof StructFieldWrapper) return true;
//		assert childWrapper.field.type != null;
//		return List.class.isAssignableFrom(childWrapper.field.type);
//	}
//
//	public <T> BitConnection createChildConnection(
//			T child, BitFieldWrapper childWrapper, Consumer<BitStructConnection.ChangeListener> reportChanges
//	) {
//		if (childWrapper instanceof StructFieldWrapper) {
//			return createRawStructConnection(child, childWrapper.field.type, reportChanges);
//		}
//		assert childWrapper.field.type != null;
//		if (List.class.isAssignableFrom(childWrapper.field.type)) {
//			//noinspection unchecked
//			return new BitListConnection<>(this, (List<T>) child, childWrapper.getChildWrapper(), reportChanges);
//		}
//		return null;
//	}

	public boolean deepEquals(Object a, Object b) {
		// TODO Recursive -> iterative
		if (a == null && b == null) return true;
		if (a == null || b == null) return false;
		BitStructWrapper<?> wrapperA = cache.getWrapper(a.getClass());
		BitStructWrapper<?> wrapperB = cache.getWrapper(b.getClass());
		return wrapperA == wrapperB && wrapperA.deepEquals(a, b, cache);
	}

	public int hashCode(Object value) {
		// TODO Recursive -> iterative
		if (value == null) return 0;
		return cache.getWrapper(value.getClass()).hashCode(value, cache);
	}
}
