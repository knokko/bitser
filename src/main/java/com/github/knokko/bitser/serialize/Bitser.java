package com.github.knokko.bitser.serialize;

import com.github.knokko.bitser.backward.LegacyClasses;
import com.github.knokko.bitser.backward.instance.LegacyStructInstance;
import com.github.knokko.bitser.connection.BitConnection;
import com.github.knokko.bitser.connection.BitListConnection;
import com.github.knokko.bitser.connection.BitStructConnection;
import com.github.knokko.bitser.context.*;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.field.FunctionContext;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.io.LayeredBitOutputStream;
import com.github.knokko.bitser.util.Recursor;
import com.github.knokko.bitser.util.RecursorException;
import com.github.knokko.bitser.util.ReferenceIdLoader;
import com.github.knokko.bitser.util.ReferenceIdMapper;
import com.github.knokko.bitser.wrapper.BitFieldWrapper;
import com.github.knokko.bitser.wrapper.BitStructWrapper;
import com.github.knokko.bitser.wrapper.StructFieldWrapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

public class Bitser {

	public static final Object BACKWARD_COMPATIBLE = new Object();

	public final BitserCache cache;

	public Bitser(boolean threadSafe) {
		this.cache = new BitserCache(threadSafe);
	}

	public void serialize(
			Object object, BitOutputStream output, Object... withAndOptions
	) throws IOException, InvalidBitValueException, InvalidBitFieldException, RecursorException {
		try {
			rawSerialize(object, output, withAndOptions);
		} catch (RecursorException failure) {
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
			throw failure;
		}
	}

	private void rawSerialize(Object object, BitOutputStream output, Object... withAndOptions) throws IOException {
		BitStructWrapper<?> wrapper = cache.getWrapper(object.getClass());

		boolean backwardCompatible = false;
		Map<String, Object> withParameters = new HashMap<>();
		for (Object withObject : withAndOptions) {
			if (withObject == BACKWARD_COMPATIBLE) backwardCompatible = true;
			if (withObject instanceof WithParameter) {
				WithParameter parameter = (WithParameter) withObject;
				if (withParameters.containsKey(parameter.key)) {
					throw new IllegalArgumentException("Duplicate with parameter " + parameter.key);
				}
				withParameters.put(parameter.key, parameter.value);
			}
		}

		LabelCollection labels = new LabelCollection(
				cache, new HashSet<>(), backwardCompatible, new FunctionContext(this, withParameters)
		);

		LegacyClasses legacy = null;
		if (backwardCompatible) {
			legacy = new LegacyClasses();

			final LegacyClasses rememberLegacy = legacy;
			rememberLegacy.setRoot(Recursor.compute(
					legacy, new LegacyInfo(cache, labels.functionContext),
					recursor -> wrapper.registerClasses(object, recursor)
			).get());

			ByteArrayOutputStream legacyBytes = new ByteArrayOutputStream();
			output.pushContext("legacy-classes", -1);
			BitOutputStream legacyOutput = new LayeredBitOutputStream(legacyBytes, output);
			serialize(legacy, legacyOutput, new WithParameter("legacy-classes", legacy));
			legacyOutput.finish();
			output.popContext("legacy-classes", -1);

			deserializeFromBytes(LegacyClasses.class, legacyBytes.toByteArray()).collectReferenceLabels(labels);
		} else wrapper.collectReferenceLabels(labels);

		for (Object withObject : withAndOptions) {
			if (withObject instanceof WithParameter || withObject == BACKWARD_COMPATIBLE) continue;
			cache.getWrapper(withObject.getClass()).collectReferenceLabels(new LabelCollection(
					cache, labels.declaredTargets, false, labels.functionContext
			));
		}

		ReferenceIdMapper idMapper = new ReferenceIdMapper(labels);
		Recursor.run(idMapper, cache, recursor ->
				wrapper.registerReferenceTargets(object, recursor)
		);
		for (Object withObject : withAndOptions) {
			if (withObject instanceof WithParameter || withObject == BACKWARD_COMPATIBLE) continue;
			Recursor.run(idMapper, cache, recursor ->
					cache.getWrapper(withObject.getClass()).registerReferenceTargets(withObject, recursor)
			);
		}

		output.pushContext("id-mapper", -1);
		idMapper.save(output);
		output.popContext("id-mapper", -1);

		output.pushContext("output", -1);
		Recursor.run(
				new WriteContext(output, idMapper),
				new WriteInfo(this, withParameters, legacy),
				recursor -> wrapper.write(object, recursor)
		);
		output.popContext("output", -1);
	}

	public byte[] serializeToBytes(Object object, Object... withAndOptions) {
		try {
			ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
			BitOutputStream bitOutput = new BitOutputStream(byteOutput);
			serialize(object, bitOutput, withAndOptions);
			bitOutput.finish();
			return byteOutput.toByteArray();
		} catch (IOException shouldNotHappen) {
			throw new Error(shouldNotHappen);
		}
	}

	public <T> T deserialize(
			Class<T> objectClass, BitInputStream input, Object... withAndOptions
	) throws IOException, InvalidBitValueException, InvalidBitFieldException, RecursorException {
		try {
			return rawDeserialize(objectClass, input, withAndOptions);
		} catch (RecursorException failure) {
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
			throw failure;
		}
	}

	private <T> T rawDeserialize(Class<T> objectClass, BitInputStream input, Object... withAndOptions) throws IOException {
		Map<String, Object> withParameters = new HashMap<>();
		boolean backwardCompatible = false;
		for (Object withObject : withAndOptions) {
			if (withObject == BACKWARD_COMPATIBLE) {
				backwardCompatible = true;
				break;
			}
		}
		LegacyClasses legacy = null;
		if (backwardCompatible) legacy = deserialize(LegacyClasses.class, input);

		BitStructWrapper<T> wrapper = cache.getWrapper(objectClass);

		LabelCollection labels = new LabelCollection(
				cache, new HashSet<>(), backwardCompatible, null
		);
		if (legacy == null) wrapper.collectReferenceLabels(labels);
		else legacy.collectReferenceLabels(labels);

		for (Object withObject : withAndOptions) {
			if (withObject == BACKWARD_COMPATIBLE) continue;
			if (withObject instanceof WithParameter) {
				WithParameter parameter = (WithParameter) withObject;
				if (withParameters.containsKey(parameter.key)) {
					throw new IllegalArgumentException("Duplicate with parameter " + parameter.key);
				}
				withParameters.put(parameter.key, parameter.value);
				continue;
			}
			cache.getWrapper(withObject.getClass()).collectReferenceLabels(new LabelCollection(
					cache, labels.declaredTargets, false,
					new FunctionContext(this, withParameters)
			));
		}

		ReferenceIdMapper withMapper = new ReferenceIdMapper(labels);
		for (Object withObject : withAndOptions) {
			if (withObject instanceof WithParameter || withObject == BACKWARD_COMPATIBLE) continue;
			Recursor.run(withMapper, cache, recursor ->
					cache.getWrapper(withObject.getClass()).registerReferenceTargets(withObject, recursor)
			);
		}

		ReferenceIdLoader idLoader = ReferenceIdLoader.load(input, labels);

		List<T> result = new ArrayList<>(1);
		ReadContext readContext = new ReadContext(input, idLoader);
		ReadInfo readInfo = new ReadInfo(this, withParameters, backwardCompatible);
		if (legacy != null) {
			LegacyStructInstance[] pLegacy = { null };
			final LegacyClasses rememberLegacy = legacy;
			Recursor.run(readContext, readInfo, recursor ->
					rememberLegacy.getRoot().read(recursor, -1, element -> pLegacy[0] = element)
			);
			Recursor.run(readContext, readInfo, recursor ->
					wrapper.fixLegacyTypes(recursor, pLegacy[0])
			);
			//noinspection unchecked
			result.add((T) pLegacy[0].newInstance);
			withMapper.shareWith(idLoader);
			idLoader.resolve();
			Recursor.run(readContext, readInfo, recursor ->
					wrapper.setLegacyValues(recursor, pLegacy[0])
			);
			idLoader.postResolve();
		} else {
			//noinspection unchecked
			Recursor.run(readContext, readInfo, recursor ->
					wrapper.read(recursor, element -> result.add((T) element))
			);
			withMapper.shareWith(idLoader);
			idLoader.resolve();
			idLoader.postResolve();
		}

		return result.get(0);
	}

	public <T> T deserializeFromBytes(Class<T> objectClass, byte[] bytes, Object... withAndOptions) {
		try {
			return deserialize(objectClass, new BitInputStream(new ByteArrayInputStream(bytes)), withAndOptions);
		} catch (IOException shouldNotHappen) {
			throw new Error(shouldNotHappen);
		}
	}

	public <T> T shallowCopy(T object) {
		//noinspection unchecked
		return (T) cache.getWrapper(object.getClass()).shallowCopy(object);
	}

	public <T> T deepCopy(T object, Object... with) {
		//noinspection unchecked
		return (T) deserializeFromBytes(object.getClass(), serializeToBytes(object, with), with);
	}

	public <T> BitStructConnection<T> createStructConnection(
			T initialState, Consumer<BitStructConnection.ChangeListener> reportChanges
	) {
		return createRawStructConnection(initialState, initialState.getClass(), reportChanges);
	}

	private <T> BitStructConnection<T> createRawStructConnection(
			T initialState, Class<?> theClass, Consumer<BitStructConnection.ChangeListener> reportChanges
	) {
		return cache.getWrapper(theClass).createConnection(this, initialState, reportChanges);
	}

	public boolean needsChildConnection(BitFieldWrapper childWrapper) {
		if (childWrapper instanceof StructFieldWrapper) return true;
		assert childWrapper.field.type != null;
		return List.class.isAssignableFrom(childWrapper.field.type);
	}

	public <T> BitConnection createChildConnection(
			T child, BitFieldWrapper childWrapper, Consumer<BitStructConnection.ChangeListener> reportChanges
	) {
		if (childWrapper instanceof StructFieldWrapper) {
			return createRawStructConnection(child, childWrapper.field.type, reportChanges);
		}
		assert childWrapper.field.type != null;
		if (List.class.isAssignableFrom(childWrapper.field.type)) {
			//noinspection unchecked
			return new BitListConnection<>(this, (List<T>) child, childWrapper.getChildWrapper(), reportChanges);
		}
		return null;
	}

	public boolean deepEquals(Object a, Object b) {
		if (a == null && b == null) return true;
		if (a == null || b == null) return false;
		BitStructWrapper<?> wrapperA = cache.getWrapper(a.getClass());
		BitStructWrapper<?> wrapperB = cache.getWrapper(b.getClass());
		return wrapperA == wrapperB && wrapperA.deepEquals(a, b, cache);
	}

	public int hashCode(Object value) {
		if (value == null) return 0;
		return cache.getWrapper(value.getClass()).hashCode(value, cache);
	}
}
