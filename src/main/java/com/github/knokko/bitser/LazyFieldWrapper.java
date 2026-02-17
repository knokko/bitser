package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.exceptions.LegacyBitserException;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.legacy.LegacyStructInstance;
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
		if (value instanceof SimpleLazyBits<?> lazy) {
			List<Object> options = new ArrayList<>();
			if (serializer.backwardCompatible) options.add(Bitser.BACKWARD_COMPATIBLE);
			if (serializer.forbidLazySaving) options.add(Bitser.FORBID_LAZY_SAVING);
			if (serializer.floatDistribution != null) options.add(serializer.floatDistribution);
			if (serializer.intDistribution != null) options.add(serializer.intDistribution);

			byte[] bytes = lazy.bytes;
			if (bytes == null || serializer.forbidLazySaving) {
				bytes = serializer.bitser.toBytes(lazy.get(), options.toArray());
			}
			serializer.output.prepareProperty("lazy-bytes-length");
			encodeUnknownLength(bytes.length, serializer.output);
			serializer.output.finishProperty();
			serializer.output.prepareProperty("lazy-bytes");
			serializer.output.write(bytes);
			serializer.output.finishProperty();
		} else {
			throw new InvalidBitValueException("Expected instance of SimpleLazyBits, but got " + value);
		}
	}

	private byte[] readLazyBytes(BitInputStream input, CollectionSizeLimit sizeLimit) throws IOException {
		input.prepareProperty("lazy-bytes-length");
		int size = decodeUnknownLength(sizeLimit, "lazy byte[] size", input);
		input.finishProperty();

		byte[] bytes = new byte[size];
		input.prepareProperty("lazy-bytes");
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
					((LegacyLazyBytes) rawLegacyInstance).bytes(),
					deserializer.bitser,
					true,
					valueClass
			);
		} else if (rawLegacyInstance instanceof LegacyStructInstance legacyObject) {
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

	@Override
	Object deepCopy(
			Object original, DeepCopyMachine machine,
			RecursionNode parentNode, String fieldName
	) {
		return ((SimpleLazyBits<?>) original).deepCopy(machine.bitser);
	}

	@Override
	void collectInstances(InstanceCollector collector, Object value, RecursionNode parentNode, String fieldName) {
		var wrapper = collector.bitser.cache.getWrapper(valueClass);
		var wrappedValue = ((SimpleLazyBits<?>) value).get();
		collector.register(wrappedValue);
		collector.structJobs.add(new CollectFromStructJob(
				wrappedValue, wrapper, new RecursionNode(parentNode, fieldName))
		);
	}

	@Override
	void hashCode(HashComputer computer, Object value, RecursionNode parentNode, String fieldName) {
		if (value != null) {
			var lazy = (SimpleLazyBits<?>) value;
			var wrapped = lazy.get();
			computer.structJobs.add(new HashStructJob(
					wrapped, computer.bitser.cache.getWrapper(wrapped.getClass()),
					new RecursionNode(parentNode, fieldName)
			));
		} else computer.digest.update((byte) 37);
	}

	@Override
	boolean certainlyNotEqual(
			DeepComparator comparator, Object valueA, Object valueB,
			RecursionNode node, String fieldName
	) {
		var lazyA = (SimpleLazyBits<?>) valueA;
		var lazyB = (SimpleLazyBits<?>) valueB;
		var wrappedA = lazyA.get();
		var wrappedB = lazyB.get();

		var wrapper = comparator.bitser.cache.getWrapper(valueClass);
		comparator.structJobs.add(new DeepCompareStructsJob(
				wrappedA, wrappedB, wrapper, new RecursionNode(node, fieldName)
		));
		return false;
	}
}
