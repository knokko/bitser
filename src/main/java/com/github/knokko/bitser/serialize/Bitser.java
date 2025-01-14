package com.github.knokko.bitser.serialize;

import com.github.knokko.bitser.connection.BitStructConnection;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.util.ReferenceIdLoader;
import com.github.knokko.bitser.util.ReferenceIdMapper;
import com.github.knokko.bitser.wrapper.BitserWrapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

public class Bitser {

	public final BitserCache cache;

	public Bitser(boolean threadSafe) {
		this.cache = new BitserCache(threadSafe);
	}

	public void serialize(Object object, BitOutputStream output, Object... with) throws IOException {
		BitserWrapper<?> wrapper = cache.getWrapper(object.getClass());

		Set<String> declaredTargetLabels = new HashSet<>();
		Set<String> stableLabels = new HashSet<>();
		Set<String> unstableLabels = new HashSet<>();
		wrapper.collectReferenceTargetLabels(cache, declaredTargetLabels, stableLabels, unstableLabels, new HashSet<>());

		for (Object withObject : with) {
			cache.getWrapper(withObject.getClass()).collectReferenceTargetLabels(
					cache, declaredTargetLabels, new HashSet<>(), new HashSet<>(), new HashSet<>()
			);
		}

		ReferenceIdMapper idMapper = new ReferenceIdMapper(declaredTargetLabels, stableLabels, unstableLabels);
		wrapper.registerReferenceTargets(object, cache, idMapper);
		for (Object withObject : with) {
			cache.getWrapper(withObject.getClass()).registerReferenceTargets(withObject, cache, idMapper);
		}

		idMapper.save(output);

		wrapper.write(object, output, cache, idMapper);
	}

	public byte[] serializeToBytes(Object object, Object... with) {
		try {
			ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
			BitOutputStream bitOutput = new BitOutputStream(byteOutput);
			serialize(object, bitOutput, with);
			bitOutput.finish();
			return byteOutput.toByteArray();
		} catch (IOException shouldNotHappen) {
			throw new Error(shouldNotHappen);
		}
	}

	public <T> T deserialize(Class<T> objectClass, BitInputStream input, Object... with) throws IOException {
		BitserWrapper<T> wrapper = cache.getWrapper(objectClass);

		Set<String> declaredTargetLabels = new HashSet<>();
		Set<String> stableLabels = new HashSet<>();
		Set<String> unstableLabels = new HashSet<>();
		wrapper.collectReferenceTargetLabels(cache, declaredTargetLabels, stableLabels, unstableLabels, new HashSet<>());

		for (Object withObject : with) {
			cache.getWrapper(withObject.getClass()).collectReferenceTargetLabels(
					cache, declaredTargetLabels, new HashSet<>(), new HashSet<>(), new HashSet<>()
			);
		}

		ReferenceIdMapper withMapper = new ReferenceIdMapper(declaredTargetLabels, stableLabels, unstableLabels);
		for (Object withObject : with) {
			cache.getWrapper(withObject.getClass()).registerReferenceTargets(withObject, cache, withMapper);
		}

		ReferenceIdLoader idLoader = ReferenceIdLoader.load(input, declaredTargetLabels, stableLabels, unstableLabels);

		List<T> result = new ArrayList<>(1);
		//noinspection unchecked
		wrapper.read(input, cache, idLoader, element -> result.add((T) element));

		withMapper.shareWith(idLoader);
		idLoader.resolve();

		return result.get(0);
	}

	public <T> T deserializeFromBytes(Class<T> objectClass, byte[] bytes, Object... with) {
		try {
			return deserialize(objectClass, new BitInputStream(new ByteArrayInputStream(bytes)), with);
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
		return (T) deserializeFromBytes(object.getClass(), serializeToBytes(object, with));
	}

	public <T> BitStructConnection<T> createStructConnection(
			T initialState, Consumer<BitStructConnection.ChangeListener> reportChanges
	) {
		return cache.getWrapper(initialState.getClass()).createConnection(this, initialState, reportChanges);
	}
}
