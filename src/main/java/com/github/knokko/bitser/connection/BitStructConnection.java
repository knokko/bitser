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

public class BitStructConnection<T> extends BitConnection {

	private final Bitser bitser;
	private final List<BitFieldWrapper> fields;
	private T referenceState;
	public final T state;
	private final Consumer<ChangeListener> reportChanges;
	private final BitConnection[] childConnections;

	public BitStructConnection(Bitser bitser, List<BitFieldWrapper> fields, T state, Consumer<ChangeListener> reportChanges) {
		this.bitser = bitser;
		this.fields = fields;
		this.referenceState = bitser.shallowCopy(state);
		this.state = state;
		this.reportChanges = reportChanges;
		this.childConnections = new BitConnection[fields.size()];
		for (BitFieldWrapper field : fields) updateChildConnection(field);
	}

	@Override
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

	@Override
	public void handleChanges(BitInputStream input) throws IOException {
		synchronized (state) {
			// TODO What about the with?
			if (input.read()) {
				int ordering = (int) decodeUniformInteger(0, fields.size() - 1, input);
				updateChildConnection(fields.get(ordering));
				if (childConnections[ordering] != null) childConnections[ordering].handleChanges(input);
				else throw new IllegalArgumentException("Unexpected ordering " + ordering);
			} else {
				for (BitFieldWrapper fieldWrapper : fields) {
					if (input.read()) {
						fieldWrapper.read(
								input, bitser.cache, null,
								value -> fieldWrapper.field.setValue.accept(state, value)
						);
					}
				}
				referenceState = bitser.shallowCopy(state);
			}
		}
	}

	@Override
	Object getState() {
		return state;
	}

	private void writeNestedChange(Object actualChild, int ordering, ChangeListener listener) {
		synchronized (state) {
			BitConnection childConnection = updateChildConnection(fields.get(ordering));
			if (childConnection == null || childConnection.getState() != actualChild) return;
			reportChanges.accept(output -> {
				output.write(true);
				encodeUniformInteger(ordering, 0, fields.size() - 1, output);
				return listener.report(output);
			});
		}
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
					updateChildConnection(fieldWrapper);
				}
			}
		}

		return numChanges;
	}

	public <C> BitListConnection<C> getChildList(String fieldName) {
		//noinspection unchecked
		return (BitListConnection<C>) getChildConnection(fieldName);
	}

	public <C> BitStructConnection<C> getChildStruct(String fieldName) {
		//noinspection unchecked
		return (BitStructConnection<C>) getChildConnection(fieldName);
	}

	private BitConnection getChildConnection(String fieldName) {
		for (BitFieldWrapper fieldWrapper : fields) {
			if (!fieldName.equals(fieldWrapper.getFieldName())) continue;
			return updateChildConnection(fieldWrapper);
		}
		throw new IllegalArgumentException("Can't find field with name " + fieldName);
	}

	private BitConnection updateChildConnection(BitFieldWrapper fieldWrapper) {
		boolean isStruct = fieldWrapper instanceof StructFieldWrapper;
		boolean isList = List.class.isAssignableFrom(fieldWrapper.field.type);
		if (isStruct || isList) {

			int ordering = fieldWrapper.field.ordering;
			synchronized (state) {
				BitConnection oldConnection = childConnections[ordering];
				BitConnection newConnection = oldConnection;
				Object currentState = fieldWrapper.field.getValue.apply(state);
				if (currentState == null) {
					newConnection = null;
				} else if (oldConnection == null || oldConnection.getState() != currentState) {
					if (isStruct) {
						newConnection = bitser.createStructConnection(
								currentState, listener -> writeNestedChange(currentState, ordering, listener)
						);
					} else {
						newConnection = new BitListConnection<>(
								bitser, (List<?>) currentState, fields.get(ordering).getChildWrapper(),
								listener -> writeNestedChange(currentState, ordering, listener)
						);
					}
				}
				childConnections[ordering] = newConnection;
				return newConnection;
			}
		} else return null;
	}

	@FunctionalInterface
	public interface ChangeListener {
		int report(BitOutputStream output) throws IOException;
	}
}
