package com.github.knokko.bitser.connection;

import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.Bitser;
import com.github.knokko.bitser.wrapper.BitFieldWrapper;
import com.github.knokko.bitser.wrapper.StructFieldWrapper;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static com.github.knokko.bitser.serialize.IntegerBitser.*;

public class BitStructConnection<T> {

	private final Bitser bitser;
	private final List<BitFieldWrapper> fields;
	private T referenceState;
	public final T state;
	private final Consumer<ChangeListener> reportChanges;
	private final BitStructConnection<?>[] childStructs;

	public BitStructConnection(Bitser bitser, List<BitFieldWrapper> fields, T state, Consumer<ChangeListener> reportChanges) {
		this.bitser = bitser;
		this.fields = fields;
		this.referenceState = bitser.shallowCopy(state);
		this.state = state;
		this.reportChanges = reportChanges;
		this.childStructs = new BitStructConnection[fields.size()];
		for (int index = 0; index < fields.size(); index++) {
			int ordering = index;
			Object child = fields.get(index).field.getValue.apply(state);
			if (fields.get(index) instanceof StructFieldWrapper) {
				childStructs[index] = bitser.createStructConnection(
						child, listener -> writeNestedChange(ordering, listener)
				);
			}
		}
	}

	public void checkForChanges() {
		synchronized (state) {
			// TODO What about the with?
			try {
				if (findAndWriteChanges(null) == 0) return;
			} catch (IOException shouldNotHappen) {
				throw new RuntimeException(shouldNotHappen);
			}
			reportChanges.accept(output -> {
				output.write(false);
				return findAndWriteChanges(output);
			});
			referenceState = bitser.shallowCopy(state);
		}
	}

	public void handleChanges(BitInputStream input) throws IOException {
		synchronized (state) {
			// TODO What about the with?
			if (input.read()) {
				int ordering = (int) decodeUniformInteger(0, fields.size() - 1, input);
				childStructs[ordering].handleChanges(input);
			} else {
				for (BitFieldWrapper fieldWrapper : fields) {
					if (input.read()) fieldWrapper.read(state, input, bitser.cache, null);
				}
				referenceState = bitser.shallowCopy(state);
			}
		}
	}

	private void writeNestedChange(int ordering, ChangeListener listener) {
		reportChanges.accept(output -> {
			output.write(true);
			encodeUniformInteger(ordering, 0, fields.size() - 1, output);
			return listener.report(output);
		});
	}

	private int findAndWriteChanges(BitOutputStream output) throws IOException {
		int numChanges = 0;
		for (BitFieldWrapper fieldWrapper : fields) {
			Class<?> fieldType = fieldWrapper.field.type;
			Object originalValue = fieldWrapper.field.getValue.apply(referenceState);
			Object modifiedValue = fieldWrapper.field.getValue.apply(state);

			boolean hasChanged;
			if (fieldType.isPrimitive() || Number.class.isAssignableFrom(fieldType) || fieldType == Boolean.class) {
				hasChanged = !Objects.equals(modifiedValue, fieldWrapper.field.getValue.apply(referenceState));
			} else {
				hasChanged = modifiedValue != originalValue;
			}

			if (output != null) output.write(hasChanged);

			if (hasChanged) {
				numChanges += 1;

				if (output != null) {
					// TODO Test (reference fields)
					fieldWrapper.write(state, output, bitser.cache, null);
				}
			}
		}

		return numChanges;
	}

	// TODO create child list

	public <C> BitStructConnection<C> getChildStruct(String fieldName) {
		for (int index = 0; index < fields.size(); index++) {
			if (fieldName.equals(fields.get(index).field.annotations.getFieldName())) {
				//noinspection unchecked
				return (BitStructConnection<C>) childStructs[index];
			}
		}
		throw new IllegalArgumentException("No child struct with name " + fieldName);
	}

	@FunctionalInterface
	public interface ChangeListener {
		int report(BitOutputStream output) throws IOException;
	}
}
