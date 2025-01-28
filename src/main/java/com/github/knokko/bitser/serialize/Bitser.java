package com.github.knokko.bitser.serialize;

import com.github.knokko.bitser.backward.LegacyClasses;
import com.github.knokko.bitser.connection.BitConnection;
import com.github.knokko.bitser.connection.BitListConnection;
import com.github.knokko.bitser.connection.BitStructConnection;
import com.github.knokko.bitser.field.FunctionContext;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.util.ReferenceIdLoader;
import com.github.knokko.bitser.util.ReferenceIdMapper;
import com.github.knokko.bitser.wrapper.BitFieldWrapper;
import com.github.knokko.bitser.wrapper.BitserWrapper;
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

	public void serialize(Object object, BitOutputStream output, Object... withAndOptions) throws IOException {
		BitserWrapper<?> wrapper = cache.getWrapper(object.getClass());

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

		LegacyClasses legacy = null;
		if (backwardCompatible) {
			legacy = new LegacyClasses();
			legacy.cache = cache;
			legacy.functionContext = new FunctionContext(withParameters);
			legacy.setRoot(wrapper.registerClasses(object, legacy));
			serialize(legacy, output, new WithParameter("legacy-classes", legacy));
		}

		LabelCollection labels = new LabelCollection(cache, new HashSet<>(), backwardCompatible);
		wrapper.collectReferenceTargetLabels(labels);

		for (Object withObject : withAndOptions) {
			if (withObject instanceof WithParameter || withObject == BACKWARD_COMPATIBLE) continue;
			cache.getWrapper(withObject.getClass()).collectReferenceTargetLabels(
					new LabelCollection(cache, labels.declaredTargets, false)
			);
		}

		ReferenceIdMapper idMapper = new ReferenceIdMapper(labels);
		wrapper.registerReferenceTargets(object, cache, idMapper);
		for (Object withObject : withAndOptions) {
			if (withObject instanceof WithParameter || withObject == BACKWARD_COMPATIBLE) continue;
			cache.getWrapper(withObject.getClass()).registerReferenceTargets(withObject, cache, idMapper);
		}

		idMapper.save(output);

		wrapper.write(object, new WriteJob(output, cache, idMapper, withParameters, legacy));
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

	public <T> T deserialize(Class<T> objectClass, BitInputStream input, Object... withAndOptions) throws IOException {
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

		BitserWrapper<T> wrapper = cache.getWrapper(objectClass);

		LabelCollection labels = new LabelCollection(cache, new HashSet<>(), backwardCompatible);
		if (legacy == null) wrapper.collectReferenceTargetLabels(labels);
		else legacy.collectReferenceTargetLabels(labels);

		for (Object withObject : withAndOptions) {
			if (withObject == BACKWARD_COMPATIBLE) withObject = legacy;
			if (withObject instanceof WithParameter) {
				WithParameter parameter = (WithParameter) withObject;
				if (withParameters.containsKey(parameter.key)) {
					throw new IllegalArgumentException("Duplicate with parameter " + parameter.key);
				}
				withParameters.put(parameter.key, parameter.value);
				continue;
			}
			cache.getWrapper(withObject.getClass()).collectReferenceTargetLabels(
					new LabelCollection(cache, labels.declaredTargets, false)
			);
		}

		ReferenceIdMapper withMapper = new ReferenceIdMapper(labels);
		for (Object withObject : withAndOptions) {
			if (withObject instanceof WithParameter || withObject == BACKWARD_COMPATIBLE) continue;
			cache.getWrapper(withObject.getClass()).registerReferenceTargets(withObject, cache, withMapper);
		}

		ReferenceIdLoader idLoader = ReferenceIdLoader.load(input, labels);

		List<T> result = new ArrayList<>(1);
		//noinspection unchecked
		wrapper.read(
				new ReadJob(input, cache, idLoader, withParameters, backwardCompatible),
				element -> result.add((T) element), legacy != null ? legacy.getRoot() : null
		);

		withMapper.shareWith(idLoader);
		idLoader.resolve();

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
}
