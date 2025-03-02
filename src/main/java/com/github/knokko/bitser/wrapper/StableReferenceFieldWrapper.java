package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.serialize.LabelCollection;
import com.github.knokko.bitser.serialize.ReadJob;
import com.github.knokko.bitser.serialize.WriteJob;
import com.github.knokko.bitser.util.VirtualField;

import java.io.IOException;
import java.util.UUID;
import java.util.function.Consumer;

import static com.github.knokko.bitser.serialize.IntegerBitser.decodeUniformInteger;

@BitStruct(backwardCompatible = false)
class StableReferenceFieldWrapper extends ReferenceFieldWrapper {

	StableReferenceFieldWrapper(VirtualField field, String label) {
		super(field, label);
	}

	@SuppressWarnings("unused")
	private StableReferenceFieldWrapper() {
		super();
	}

	@Override
	public void collectReferenceTargetLabels(LabelCollection labels) {
		super.collectReferenceTargetLabels(labels);
		labels.stable.add(label);
	}

	@Override
	void writeValue(Object value, WriteJob write) throws IOException {
		write.idMapper.encodeStableId(label, value, write.output, write.cache);
	}

	@Override
	void readValue(ReadJob read, ValueConsumer setValue) throws IOException {
		read.idLoader.getStable(label, setValue, read.input);
	}

	@Override
	Object readLegacyValue(ReadJob read) throws IOException {
		return new UUID(
				decodeUniformInteger(Long.MIN_VALUE, Long.MAX_VALUE, read.input),
				decodeUniformInteger(Long.MIN_VALUE, Long.MAX_VALUE, read.input)
		);
	}

	@Override
	void setLegacyValue(ReadJob read, Object value, Consumer<Object> setValue) {
		if (value == null) {
			super.setLegacyValue(read, null, setValue);
			return;
		}

		setValue.accept(read.idLoader.getStableNow(label, (UUID) value));
	}
}
