package com.github.knokko.bitser.serialize;

import com.github.knokko.bitser.backward.LegacyClasses;
import com.github.knokko.bitser.backward.instance.LegacyStructInstance;
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

		LabelCollection labels = new LabelCollection(
				cache, new HashSet<>(), backwardCompatible, new FunctionContext(this, withParameters)
		);
		wrapper.collectReferenceLabels(labels);

		LegacyClasses legacy = null;
		if (backwardCompatible) {
			legacy = new LegacyClasses();
			legacy.cache = cache;
			legacy.functionContext = labels.functionContext;
			legacy.setRoot(wrapper.registerClasses(object, legacy));

			LabelCollection usedLabels = new LabelCollection(
					cache, new HashSet<>(), false, legacy.functionContext
			);
			wrapper.collectUsedReferenceLabels(usedLabels, object);
			for (String label : labels.unstable) {
				if (!usedLabels.unstable.contains(label)) legacy.addUnstableLabel(label);
			}

			serialize(legacy, output, new WithParameter("legacy-classes", legacy));
		}

		for (Object withObject : withAndOptions) {
			if (withObject instanceof WithParameter || withObject == BACKWARD_COMPATIBLE) continue;
			cache.getWrapper(withObject.getClass()).collectReferenceLabels(new LabelCollection(
					cache, labels.declaredTargets, false, labels.functionContext
			));
		}

		ReferenceIdMapper idMapper = new ReferenceIdMapper(labels);
		wrapper.registerReferenceTargets(object, cache, idMapper);
		for (Object withObject : withAndOptions) {
			if (withObject instanceof WithParameter || withObject == BACKWARD_COMPATIBLE) continue;
			cache.getWrapper(withObject.getClass()).registerReferenceTargets(withObject, cache, idMapper);
		}

		idMapper.save(output);

		wrapper.write(object, new WriteJob(this, output, idMapper, withParameters, legacy));
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
			cache.getWrapper(withObject.getClass()).registerReferenceTargets(withObject, cache, withMapper);
		}

		ReferenceIdLoader idLoader = ReferenceIdLoader.load(input, labels);

		List<T> result = new ArrayList<>(1);
		ReadJob readJob = new ReadJob(this, input, idLoader, withParameters, backwardCompatible);
		if (legacy != null) {
			LegacyStructInstance[] pLegacy = { null };
			legacy.getRoot().read(readJob, -1, element -> pLegacy[0] = element);
			wrapper.fixLegacyTypes(readJob, pLegacy[0]);
			//noinspection unchecked
			result.add((T) pLegacy[0].newInstance);
			withMapper.shareWith(idLoader);
			idLoader.resolve();
			wrapper.setLegacyValues(readJob, pLegacy[0]);
			idLoader.postResolve();
		} else {
			//noinspection unchecked
			wrapper.read(readJob, element -> result.add((T) element));
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

	public boolean deepEquals(Object a, Object b, Object... withAndOptions) {
		if (a.getClass() != b.getClass()) return false;
		return Arrays.equals(serializeToBytes(a, withAndOptions), serializeToBytes(b, withAndOptions));
	}

	public int hashCode(Object target, Object... withAndOptions) {
		return Arrays.hashCode(serializeToBytes(target, withAndOptions));
	}
}
