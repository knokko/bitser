package com.github.knokko.bitser;

import com.github.knokko.bitser.legacy.LegacyStructInstance;
import com.github.knokko.bitser.exceptions.*;
import com.github.knokko.bitser.field.FunctionContext;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.io.LayeredBitOutputStream;
import com.github.knokko.bitser.options.CollectionSizeLimit;
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

	public void serialize(
			Object object, BitOutputStream output, Object... withAndOptions
	) throws IOException, BitserException, RecursorException, IllegalArgumentException {
		try {
			rawSerialize(object, output, withAndOptions);
		} catch (RecursorException failure) {
			rethrowRecursorException(failure);
			throw failure;
		}
	}

	private void rawSerialize(Object object, BitOutputStream output, Object... withAndOptions) throws IOException {
		output.pushContext("prepare", -1);
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

		LabelInfo labelInfo = new LabelInfo(cache, backwardCompatible, new FunctionContext(
				this, backwardCompatible, withParameters
		));
		LabelContext labelContext = new LabelContext(new HashSet<>());

		output.popContext("prepare", -1);
		LegacyClasses legacy = null;
		if (backwardCompatible) {
			output.pushContext("register-legacy-classes", -1);
			legacy = new LegacyClasses();

			final LegacyClasses rememberLegacy = legacy;
			rememberLegacy.setRoot(Recursor.compute(
					legacy, new LegacyInfo(cache, labelInfo.functionContext), wrapper::registerClasses
			).get());

			output.popContext("register-legacy-classes", -1);
			output.pushContext("legacy-classes", -1);
			ByteArrayOutputStream legacyBytes = new ByteArrayOutputStream();
			BitOutputStream legacyOutput = new LayeredBitOutputStream(legacyBytes, output);
			serialize(legacy, legacyOutput, new WithParameter("legacy-classes", legacy));
			legacyOutput.finish();
			output.popContext("legacy-classes", -1);

			output.pushContext("legacy-reference-labels", -1);
			Recursor.run(labelContext, labelInfo, deserializeFromBytes(
					LegacyClasses.class, legacyBytes.toByteArray()
			)::collectReferenceLabels);
			output.popContext("legacy-reference-labels", -1);
		} else {
			output.pushContext("collect-reference-labels", -1);
			Recursor.run(labelContext, labelInfo, wrapper::collectReferenceLabels);
			output.popContext("collect-reference-labels", -1);
		}

		output.pushContext("with-reference-labels", -1);
		for (Object withObject : withAndOptions) {
			if (withObject == BACKWARD_COMPATIBLE || withObject == FORBID_LAZY_SAVING) continue;
			if (withObject instanceof WithParameter || withObject instanceof DistributionTracker) continue;
			Recursor.run(
					new LabelContext(labelContext.declaredTargets),
					new LabelInfo(cache, false, labelInfo.functionContext),
					cache.getWrapper(withObject.getClass())::collectReferenceLabels
			);
		}
		output.popContext("with-reference-labels", -1);

		output.pushContext("register-reference-targets", -1);
		ReferenceIdMapper idMapper = new ReferenceIdMapper(labelContext);
		Recursor.run(idMapper, cache, recursor ->
				wrapper.registerReferenceTargets(object, recursor)
		);
		output.popContext("register-reference-targets", -1);

		output.pushContext("with-reference-targets", -1);
		for (Object withObject : withAndOptions) {
			if (withObject == BACKWARD_COMPATIBLE || withObject == FORBID_LAZY_SAVING) continue;
			if (withObject instanceof WithParameter || withObject instanceof DistributionTracker) continue;
			Recursor.run(idMapper, cache, recursor ->
					cache.getWrapper(withObject.getClass()).registerReferenceTargets(withObject, recursor)
			);
		}
		output.popContext("with-reference-targets", -1);

		output.pushContext("id-mapper", -1);
		idMapper.save(output);
		output.popContext("id-mapper", -1);

		output.pushContext("output", -1);
		Recursor.run(
				new WriteContext(output, idMapper, integerDistribution, floatDistribution),
				new WriteInfo(this, withParameters, legacy, output.usesContextInfo(), forbidLazySaving),
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
			throw new UnexpectedBitserException("ByteArrayOutputStream threw an IOException?");
		}
	}

	public <T> T deserialize(
			Class<T> objectClass, BitInputStream input, Object... withAndOptions
	) throws IOException, BitserException, RecursorException, IllegalArgumentException {
		try {
			return rawDeserialize(objectClass, input, withAndOptions);
		} catch (RecursorException failure) {
			rethrowRecursorException(failure);
			throw failure;
		}
	}

	private <T> T rawDeserialize(Class<T> objectClass, BitInputStream input, Object... withAndOptions) throws IOException {
		CollectionSizeLimit sizeLimit = null;
		boolean backwardCompatible = false;
		for (Object withObject : withAndOptions) {
			if (withObject == BACKWARD_COMPATIBLE) {
				backwardCompatible = true;
				break;
			}
			if (withObject instanceof CollectionSizeLimit) {
				if (sizeLimit != null) throw new IllegalArgumentException("Encountered multiple size limits");
				sizeLimit = (CollectionSizeLimit) withObject;
			}
		}

		LegacyClasses legacy = null;
		if (backwardCompatible) legacy = deserialize(LegacyClasses.class, input, sizeLimit);

		BitStructWrapper<T> wrapper = cache.getWrapper(objectClass);

		LabelContext labelContext = new LabelContext(new HashSet<>());
		LabelInfo labelInfo = new LabelInfo(cache, backwardCompatible, null);
		Recursor.run(labelContext, labelInfo, legacy == null ? wrapper::collectReferenceLabels :
				legacy::collectReferenceLabels
		);

		Map<String, Object> withParameters = new HashMap<>();
		for (Object withObject : withAndOptions) {
			if (withObject == null || withObject == BACKWARD_COMPATIBLE || withObject  == FORBID_LAZY_SAVING) continue;
			if (withObject instanceof CollectionSizeLimit || withObject instanceof DistributionTracker) continue;
			if (withObject instanceof WithParameter) {
				WithParameter parameter = (WithParameter) withObject;
				if (withParameters.containsKey(parameter.key)) {
					throw new IllegalArgumentException("Duplicate with parameter " + parameter.key);
				}
				withParameters.put(parameter.key, parameter.value);
				continue;
			}
			Recursor.run(
					new LabelContext(labelContext.declaredTargets),
					new LabelInfo(cache, false, new FunctionContext(
							this, backwardCompatible, withParameters
					)),
					cache.getWrapper(withObject.getClass())::collectReferenceLabels
			);
		}

		ReferenceIdMapper withMapper = new ReferenceIdMapper(labelContext);
		for (Object withObject : withAndOptions) {
			if (withObject == null || withObject == BACKWARD_COMPATIBLE) continue;
			if (withObject == FORBID_LAZY_SAVING) {
				System.out.println("Ignoring FORBID_LAZY_SAVING option in Bitser.deserialize");
				continue;
			}
			if (withObject instanceof WithParameter || withObject instanceof CollectionSizeLimit) continue;
			if (withObject instanceof DistributionTracker) {
				System.out.println("Ignoring DistributionTracker option in Bitser.deserialize");
				continue;
			}

			Recursor.run(withMapper, cache, recursor ->
					cache.getWrapper(withObject.getClass()).registerReferenceTargets(withObject, recursor)
			);
		}

		ReferenceIdLoader idLoader = ReferenceIdLoader.load(input, labelContext, sizeLimit);

		List<T> result = new ArrayList<>(1);
		ReadContext readContext = new ReadContext(input, idLoader);
		ReadInfo readInfo = new ReadInfo(this, withParameters, backwardCompatible, sizeLimit);
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

	public <T> T deserializeFromBytes(
			Class<T> objectClass, byte[] bytes, Object... withAndOptions
	) throws BitserException, RecursorException, IllegalArgumentException {
		try {
			return deserialize(objectClass, new BitInputStream(new ByteArrayInputStream(bytes)), withAndOptions);
		} catch (IOException shouldNotHappen) {
			throw new IllegalArgumentException("bytes is too short or invalid/corrupted", shouldNotHappen);
		}
	}

	public <T> T shallowCopy(T object) {
		//noinspection unchecked
		return (T) cache.getWrapper(object.getClass()).shallowCopy(object);
	}

	public <T> T deepCopy(
			T object, Object... with
	) throws BitserException, RecursorException, IllegalArgumentException {
		//noinspection unchecked
		return (T) deserializeFromBytes(object.getClass(), serializeToBytes(object, with), with);
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
