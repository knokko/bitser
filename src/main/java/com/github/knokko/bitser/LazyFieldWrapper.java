package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.exceptions.LegacyBitserException;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.legacy.BackStructInstance;
import com.github.knokko.bitser.legacy.LegacyLazyBytes;
import com.github.knokko.bitser.options.CollectionSizeLimit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.github.knokko.bitser.IntegerBitser.decodeUnknownLength;
import static com.github.knokko.bitser.IntegerBitser.encodeUnknownLength;

@BitStruct(backwardCompatible = false)
class LazyFieldWrapper extends BitFieldWrapper {

	private final Class<?> valueClass;

	@SuppressWarnings("unused")
	private LazyFieldWrapper() {
		super();
		this.valueClass = null;
	}

	LazyFieldWrapper(VirtualField field, Class<?> valueClass) {
		super(field);
		this.valueClass = valueClass;
	}

	@Override
	public void write(
			Serializer serializer, Object value,
			RecursionNode parentNode, String fieldName
	) throws Throwable {
		if (value instanceof SimpleLazyBits) {
			SimpleLazyBits<?> lazy = (SimpleLazyBits<?>) value;
			List<Object> options = new ArrayList<>();
			if (serializer.backwardCompatible) options.add(Bitser.BACKWARD_COMPATIBLE);
			if (serializer.forbidLazySaving) options.add(Bitser.FORBID_LAZY_SAVING);
			if (serializer.floatDistribution != null) options.add(serializer.floatDistribution);
			if (serializer.intDistribution != null) options.add(serializer.intDistribution);

			byte[] bytes = lazy.bytes;
			if (bytes == null || serializer.forbidLazySaving) {
				bytes = serializer.bitser.serializeToBytesSimple(lazy.get(), options.toArray());
			}
			serializer.output.prepareProperty("lazy-bytes-length", -1);
			encodeUnknownLength(bytes.length, serializer.output);
			serializer.output.finishProperty();
			serializer.output.prepareProperty("lazy-bytes", -1);
			serializer.output.write(bytes);
			serializer.output.finishProperty();
		} else {
			throw new InvalidBitValueException("Expected instance of SimpleLazyBits, but got " + value);
		}
	}

	private byte[] readLazyBytes(BitInputStream input, CollectionSizeLimit sizeLimit) throws IOException {
		input.prepareProperty("lazy-bytes-length", -1);
		int size = decodeUnknownLength(sizeLimit, "lazy byte[] size", input);
		input.finishProperty();

		byte[] bytes = new byte[size];
		input.prepareProperty("lazy-bytes", -1);
		input.read(bytes);
		input.finishProperty();

		return bytes;
	}

	@Override
	public Object read(Deserializer deserializer, RecursionNode parentNode, String fieldName) throws Throwable {
		byte[] bytes = readLazyBytes(deserializer.input, deserializer.sizeLimit);
		return new SimpleLazyBits<>(bytes, deserializer.bitser, false, valueClass);
	}

	@Override
	Object read(BackDeserializer deserializer, RecursionNode parentNode, String fieldName) throws Throwable {
		byte[] bytes = readLazyBytes(deserializer.input, deserializer.sizeLimit);
		return new LegacyLazyBytes(bytes);
	}

	@Override
	Object convert(BackDeserializer deserializer, Object rawLegacyInstance, RecursionNode parentNode, String fieldName) {
		if (rawLegacyInstance instanceof LegacyLazyBytes) {
			return new SimpleLazyBits<>(
					((LegacyLazyBytes) rawLegacyInstance).bytes,
					deserializer.bitser,
					true,
					valueClass
			);
		} else if (rawLegacyInstance instanceof BackStructInstance) {
			BackStructInstance legacyObject = (BackStructInstance) rawLegacyInstance;
			BitStructWrapper<?> modernInfo = deserializer.bitser.cache.getWrapper(valueClass);
			Object modernObject = modernInfo.createEmptyInstance();
			deserializer.convertStructJobs.add(new BackConvertStructJob(
					modernObject, modernInfo, legacyObject,
					new RecursionNode(parentNode, fieldName)
			));
			return new SimpleLazyBits<>(modernObject);
		} else {
			throw new LegacyBitserException("Can't convert from legacy " + rawLegacyInstance + " to lazy for field " + field);
		}
	}
}
