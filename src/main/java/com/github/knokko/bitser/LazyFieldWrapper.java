package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.legacy.LegacyLazyBytes;
import com.github.knokko.bitser.legacy.LegacyStructInstance;
import com.github.knokko.bitser.util.Recursor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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
	void writeValue(Object value, Recursor<WriteContext, WriteInfo> recursor) {
		if (value instanceof SimpleLazyBits) {
			SimpleLazyBits<?> lazy = (SimpleLazyBits<?>) value;
			recursor.runFlat("lazy-bytes", context -> {
				List<Object> options = new ArrayList<>();
				if (recursor.info.legacy != null) options.add(Bitser.BACKWARD_COMPATIBLE);
				if (recursor.info.forbidLazySaving) options.add(Bitser.FORBID_LAZY_SAVING);
				if (context.floatDistribution != null) options.add(context.floatDistribution);
				if (context.integerDistribution != null) options.add(context.integerDistribution);

				byte[] bytes = lazy.bytes;
				if (bytes == null || recursor.info.forbidLazySaving) {
					bytes = recursor.info.bitser.serializeToBytes(lazy.get(), options.toArray());
				}
				encodeUnknownLength(bytes.length, context.output);
				context.output.write(bytes);
			});
		} else {
			throw new InvalidBitValueException("Expected instance of SimpleLazyBits, but got " + value);
		}
	}

	@Override
	public void write(
			Serializer serializer, Object value,
			RecursionNode parentNode, String fieldName
	) throws Throwable {
		if (value instanceof SimpleLazyBits) {
			SimpleLazyBits<?> lazy = (SimpleLazyBits<?>) value;
			List<Object> options = new ArrayList<>();
			//if (recursor.info.legacy != null) options.add(Bitser.BACKWARD_COMPATIBLE);
			//if (recursor.info.forbidLazySaving) options.add(Bitser.FORBID_LAZY_SAVING);
//			if (context.floatDistribution != null) options.add(context.floatDistribution);
//			if (context.integerDistribution != null) options.add(context.integerDistribution);

			byte[] bytes = lazy.bytes;
			if (bytes == null/* || recursor.info.forbidLazySaving*/) {
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

	@Override
	public Object read(Deserializer deserializer, RecursionNode parentNode, String fieldName) throws Throwable {
		deserializer.input.prepareProperty("lazy-bytes-length", -1);
		int size = decodeUnknownLength(deserializer.sizeLimit, "lazy byte[] size", deserializer.input);
		deserializer.input.finishProperty();

		byte[] bytes = new byte[size];
		deserializer.input.prepareProperty("lazy-bytes", -1);
		deserializer.input.read(bytes);
		deserializer.input.finishProperty();

		if (false/*recursor.info.backwardCompatible*/) {
			return new LegacyLazyBytes(bytes);
		} else {
			return new SimpleLazyBits<>(bytes, deserializer.bitser, false, valueClass);
		}
	}

	@Override
	void readValue(Recursor<ReadContext, ReadInfo> recursor, Consumer<Object> setValue) {
		recursor.runFlat("lazy-bytes", context -> {
			int size = decodeUnknownLength(recursor.info.sizeLimit, "lazy byte[] size", context.input);
			byte[] bytes = new byte[size];
			context.input.read(bytes);

			if (recursor.info.backwardCompatible) {
				setValue.accept(new LegacyLazyBytes(bytes));
			} else {
				setValue.accept(new SimpleLazyBits<>(bytes, recursor.info.bitser, false, valueClass));
			}
		});
	}

	@Override
	void fixLegacyTypes(Recursor<ReadContext, ReadInfo> recursor, Object value) {
		if (value instanceof LegacyStructInstance) {
			BitStructWrapper<?> valueWrapper = recursor.info.bitser.cache.getWrapper(valueClass);
			recursor.runNested("lazy legacy", nested ->
				valueWrapper.fixLegacyTypes(nested, (LegacyStructInstance) value)
			);
		} else super.fixLegacyTypes(recursor, value);
	}

	@Override
	void setLegacyValue(Recursor<ReadContext, ReadInfo> recursor, Object rawLegacyInstance, Consumer<Object> setValue) {
		if (rawLegacyInstance instanceof LegacyLazyBytes) {
			setValue.accept(new SimpleLazyBits<>(
					((LegacyLazyBytes) rawLegacyInstance).bytes,
					recursor.info.bitser,
					recursor.info.backwardCompatible,
					valueClass
			));
		} else if (rawLegacyInstance instanceof LegacyStructInstance) {
			BitStructWrapper<?> valueWrapper = recursor.info.bitser.cache.getWrapper(valueClass);
			recursor.runNested("lazy legacy", nested ->
					setValue.accept(new SimpleLazyBits<>(
							valueWrapper.setLegacyValues(nested, (LegacyStructInstance) rawLegacyInstance)
					))
			);
		} else {
			super.setLegacyValue(recursor, rawLegacyInstance, setValue);
		}
	}
}
