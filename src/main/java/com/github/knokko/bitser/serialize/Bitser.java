package com.github.knokko.bitser.serialize;

import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.util.ReferenceIdLoader;
import com.github.knokko.bitser.util.ReferenceIdMapper;
import com.github.knokko.bitser.wrapper.BitserWrapper;

import java.io.IOException;
import java.util.*;

public class Bitser {

	private final BitserCache cache;

	public Bitser(boolean threadSafe) {
		this.cache = new BitserCache(threadSafe);
	}

	public void serialize(Object object, BitOutputStream output) throws IOException {
		BitserWrapper<?> wrapper = cache.getWrapper(object.getClass());

		Set<String> labels = new HashSet<>();
		wrapper.collectReferenceTargetLabels(cache, labels, new HashSet<>());

		ReferenceIdMapper idMapper = new ReferenceIdMapper(labels);
		wrapper.registerReferenceTargets(object, cache, idMapper);

		idMapper.save(output);

		wrapper.write(object, output, cache, idMapper);
	}

	public <T> T deserialize(Class<T> objectClass, BitInputStream input) throws IOException {
		BitserWrapper<T> wrapper = cache.getWrapper(objectClass);

		Set<String> labels = new HashSet<>();
		wrapper.collectReferenceTargetLabels(cache, labels, new HashSet<>());

		ReferenceIdLoader idLoader = ReferenceIdLoader.load(input, labels);

		List<T> result = new ArrayList<>(1);
		//noinspection unchecked
		wrapper.read(input, cache, idLoader, element -> result.add((T) element));

		try {
			idLoader.resolve();
		} catch (IllegalAccessException shouldNotHappen) {
			throw new Error(shouldNotHappen);
		}

		return result.get(0);
	}
}
