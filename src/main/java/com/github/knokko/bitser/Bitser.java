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
import java.nio.ByteBuffer;
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
	) throws IOException, BitserException, IllegalArgumentException {
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
	) throws IOException, BitserException, IllegalArgumentException {
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
	) throws BitserException, IllegalArgumentException {
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
	) throws BitserException, IllegalArgumentException {
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
		if (value == null) return 0;
		var computer = new HashComputer(value, this);
		computer.feedHash();
		byte[] byteHash = computer.digest.digest();
		return ByteBuffer.wrap(byteHash).getInt(0);
	}

	/**
	 * Creates a <i>deep</i> (nested) copy of {@code original} and its children.
	 * <ul>
	 *     <li>The {@code original} must be a {@link BitStruct}</li>
	 *     <li>Only the fields annotated with {@code @BitField} will be copied.</li>
	 *     <li>
	 *         References to targets <i>within</i> {@code original} will be set to the corresponding copied target.
	 *     </li>
	 *     <li>
	 *         References to targets <i>outside</i> {@code original} (e.g. 'with' objects) will <b>not</b> be copied:
	 *         the copied reference will have the same identity as the original reference.
	 *     </li>
	 * </ul>
	 * @param original The struct to be copied
	 * @param withParameters The with parameters that should be passed to {@link BitPostInit.Context} and
	 *                       {@link com.github.knokko.bitser.field.FunctionContext}. These parameters are completely
	 *                       optional. If no (child) struct implements {@link BitPostInit}, nor has any methods
	 *                       annotated with {@code @BitField}, the {@code withParameters} are useless/ignored.
	 * @return A deep copy of {@code original}
	 * @param <T> The type of {@code original}
	 */
	public <T> T deepCopy(T original, WithParameter...withParameters) {
		Map<String, Object> withMapping = new HashMap<>();
		for (var parameter : withParameters) {
			withMapping.put(parameter.key(), parameter.value());
		}

		DeepCopyMachine machine = new DeepCopyMachine(original, this, withMapping);
		machine.run();
		//noinspection unchecked
		return (T) machine.destinationRoot;
	}

	/**
	 * <p>
	 *     Collects all instances of the keys of {@code destination} that occur inside {@code rootStruct} or any of its
	 *     children.
	 * </p>
	 * <p>
	 *     For instance, to find all instances of {@code ExampleClass} or {@code ExampleInterface}, you should create
	 *     a Map that maps {@code ExampleClass} to an empty collection, and maps {@code ExampleInterface} to
	 *     <i>another</i> empty collection. Bitser will add any instances of {@code ExampleClass} and
	 *     {@code ExampleInterface} to their respective collections, before this method returns.
	 * </p>
	 *
	 * <h3>Recommendations</h3>
	 * <p>
	 *     The concrete types of these collections are not very important. {@code ArrayList} is the most standard
	 *     choice, but you could also use e.g. {@code HashSet} to automatically eliminate duplicates.
	 * </p>
	 * <p>
	 *     It is recommended to put <i>empty</i> collections, but this is not required. If the collections are
	 *     non-empty, bitser will simply <i>add</i> the instances that it found to the non-empty collection.
	 * </p>
	 * <p>
	 *     If you put the same collection instance at two different classes/keys, that collection will contain the
	 *     instances of both classes after this method returns.
	 * </p>
	 *
	 * <h3>References</h3>
	 * <p>
	 *     This method will <b>skip</b> all reference fields that it encounters, which avoids duplicates and potentially
	 *     endless loops. The reference <b>targets</b> are treated just like any other field, so they can still be
	 *     collected. So, if there are 5 references to the same target, the target will be searched (and possibly
	 *     collected) exactly once.
	 * </p>
	 * @param rootStruct The struct object in which bitser should search for instances of the desired classes
	 * @param destination The map that should contain a key for each class of interest
	 * @param withParameters The with parameters that should be passed to
	 *                       {@link com.github.knokko.bitser.field.FunctionContext}. These parameters are completely
	 *                       optional, and only useful if there are any methods annotated with {@code @BitField} and
	 *                       a parameter of type {@code FunctionContext}.
	 */
	public void collectInstances(
			Object rootStruct,
			Map<Class<?>, Collection<Object>> destination,
			WithParameter...withParameters
	) {
		Map<String, Object> withMapping = new HashMap<>();
		for (var parameter : withParameters) {
			withMapping.put(parameter.key(), parameter.value());
		}

		for (var destinationClass : destination.keySet()) {
			if (destinationClass.isPrimitive()) {
				throw new IllegalArgumentException("Don't use primitive classes: use their boxed/wrapper type instead");
			}
		}

		new InstanceCollector(rootStruct, destination, this, withMapping).run();
	}
}
