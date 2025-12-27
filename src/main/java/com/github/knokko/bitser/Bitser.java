package com.github.knokko.bitser;

import com.github.knokko.bitser.io.*;
import com.github.knokko.bitser.exceptions.*;
import com.github.knokko.bitser.options.CollectionSizeLimit;
import com.github.knokko.bitser.options.DebugBits;
import com.github.knokko.bitser.options.WithParameter;
import com.github.knokko.bitser.distributions.*;

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

	private void rethrowRecursorException(RecursionException failure) throws IOException {
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

	public void serialize(
			Object object, BitOutputStream output, Object... withAndOptions
	) throws IOException, BitserException, RecursionException, IllegalArgumentException {
		try {
			rawSerialize(object, output, withAndOptions);
		} catch (RecursionException failure) {
			rethrowRecursorException(failure);
			throw failure;
		}
	}

	private void rawSerialize(Object object, BitOutputStream output, Object... withAndOptions) {
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
			if (withObject instanceof WithParameter parameter) {
				if (withParameters.containsKey(parameter.key())) {
					throw new IllegalArgumentException("Duplicate with parameter " + parameter.key());
				}
				withParameters.put(parameter.key(), parameter.value());
			}
		}

		if (backwardCompatible) {
			LegacyClasses legacy = new LegacyClasses();
			new UsedStructCollector(cache, legacy, object.getClass()).collect();

			try {
				RecursionNode legacyMarker = new RecursionNode("<<<<<<<<<<<<<<<<<<<< LEGACY >>>>>>>>>>>>>>>>>");
				output.pushContext(legacyMarker, null);
				serialize(legacy, output, new WithParameter("legacy-classes", legacy));
				output.popContext(legacyMarker, null);
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

	public byte[] toBytes(Object object, Object... withAndOptions) {
		DebugBits debug = null;
		for (Object option : withAndOptions) {
			if (option instanceof DebugBits) debug = (DebugBits) option;
		}

		try {
			ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
			BitOutputStream bitOutput = debug != null ?
					new DebugBitOutputStream(byteOutput, debug.writer(), debug.flushAggressively()) :
					new BitOutputStream(byteOutput);
			serialize(object, bitOutput, withAndOptions);
			bitOutput.finish();
			return byteOutput.toByteArray();
		} catch (IOException shouldNotHappen) {
			throw new UnexpectedBitserException("ByteArrayOutputStream threw an IOException?");
		}
	}

	public <T> T deserialize(
			Class<T> objectClass, BitInputStream input, Object... withAndOptions
	) throws IOException, BitserException, RecursionException, IllegalArgumentException {
		try {
			return rawDeserialize(objectClass, input, withAndOptions);
		} catch (RecursionException failure) {
			rethrowRecursorException(failure);
			throw failure;
		}
	}

	private <T> T rawDeserialize(
			Class<T> objectClass, BitInputStream input, Object... withAndOptions
	) throws IOException {
		CollectionSizeLimit sizeLimit = null;
		boolean backwardCompatible = false;
		Map<String, Object> withParameters = new HashMap<>();
		ArrayList<Object> withObjects = new ArrayList<>();

		for (Object maybeWithObject : withAndOptions) {
			if (maybeWithObject == BACKWARD_COMPATIBLE) {
				backwardCompatible = true;
				continue;
			}
			if (maybeWithObject instanceof CollectionSizeLimit) {
				if (sizeLimit != null) throw new IllegalArgumentException("Encountered multiple size limits");
				sizeLimit = (CollectionSizeLimit) maybeWithObject;
				continue;
			}
			if (maybeWithObject instanceof WithParameter parameter) {
				if (withParameters.containsKey(parameter.key())) {
					throw new IllegalArgumentException("Duplicate with parameter " + parameter.key());
				}
				withParameters.put(parameter.key(), parameter.value());
				continue;
			}
			if (maybeWithObject == null || maybeWithObject instanceof DebugBits) continue;
			if (maybeWithObject == FORBID_LAZY_SAVING) {
				System.out.println("Ignoring FORBID_LAZY_SAVING option in Bitser.deserialize");
				continue;
			}
			if (maybeWithObject instanceof DistributionTracker) {
				System.out.println("Ignoring DistributionTracker option in Bitser.deserialize");
				continue;
			}

			withObjects.add(maybeWithObject);
		}

		LegacyClasses legacy = null;
		if (backwardCompatible) {
			input.pushContext(new RecursionNode("<<<<<<<<<<<<<<<<<<<< LEGACY >>>>>>>>>>>>>>>>>"), null);
			legacy = deserialize(LegacyClasses.class, input, sizeLimit);
			input.popContext(new RecursionNode("<<<<<<<<<<<<<<<<<<<< LEGACY >>>>>>>>>>>>>>>>>"), null);
		}

		BitStructWrapper<T> wrapper = cache.getWrapper(objectClass);

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

	public <T> T fromBytes(
			Class<T> objectClass, byte[] bytes, Object... withAndOptions
	) throws BitserException, RecursionException, IllegalArgumentException {
		DebugBits debug = null;
		for (Object option : withAndOptions) {
			if (option instanceof DebugBits) debug = (DebugBits) option;
		}
		try {
			ByteArrayInputStream byteInput = new ByteArrayInputStream(bytes);
			BitInputStream bitInput = debug != null ?
					new DebugBitInputStream(byteInput, debug.writer(), debug.flushAggressively()) :
					new BitInputStream(byteInput);
			T result = deserialize(objectClass, bitInput, withAndOptions);
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
	) throws BitserException, RecursionException, IllegalArgumentException {
		//noinspection unchecked
		return (T) fromBytes(object.getClass(), toBytes(object, with), with);
	}

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
