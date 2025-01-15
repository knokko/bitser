package com.github.knokko.bitser.connection;

import com.github.knokko.bitser.BitEnum;
import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.serialize.Bitser;
import com.github.knokko.bitser.wrapper.AbstractCollectionFieldWrapper;
import com.github.knokko.bitser.wrapper.BitFieldWrapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class BitListConnection<T> extends BitConnection {

	private final Bitser bitser;
	public final List<T> list;
	private final List<T> myList;
	private final BitFieldWrapper elementWrapper;
	private final Consumer<BitStructConnection.ChangeListener> reportChanges;

	BitListConnection(
			Bitser bitser, List<T> list, BitFieldWrapper elementWrapper,
			Consumer<BitStructConnection.ChangeListener> reportChanges
	) {
		this.bitser = bitser;
		this.list = list;
		this.myList = new ArrayList<>(list);
		this.elementWrapper = elementWrapper;
		this.reportChanges = reportChanges;
	}

	private synchronized void postModification(Modification<T> modification) {
		reportChanges.accept(output -> {
			output.write(false);
			bitser.serialize(modification, output);
			if (modification.action != Action.REMOVE) {
				AbstractCollectionFieldWrapper.writeElement(
						modification.element, elementWrapper, output, bitser.cache, null,
						"This BitListConnection must not contain null values"
				);
			}
			return 1;
		});
	}

	public void addDelayed(T element) {
		postModification(new Modification<>(element, -1, Action.ADD));
	}

	public void addDelayed(int index, T element) {
		postModification(new Modification<>(element, index, Action.ADD));
	}

	public void replaceDelayed(int index, T element) {
		postModification(new Modification<>(element, index, Action.REPLACE));
	}

	public void removeDelayed(int index) {
		postModification(new Modification<>(null, index, Action.REMOVE));
	}

	@Override
	public void checkForChanges() {
		synchronized (list) {
			if (list.size() != myList.size()) throw new Error("Detected external modification");
		}
	}

	@Override
	public void handleChanges(BitInputStream input) throws IOException {
		if (list.size() != myList.size()) throw new Error("Detected external modification");
		if (input.read()) {
			// TODO Read nested change
		} else {
			Modification<?> modification = bitser.deserialize(Modification.class, input);
			if (modification.action == Action.REMOVE) {
				synchronized (list) {
					if (modification.index < myList.size()) {
						myList.remove(modification.index);
						list.remove(modification.index);
					} else System.out.println("BitList removal failed");
				}
			} else {
				elementWrapper.read(input, bitser.cache, null, rawElement -> {
					@SuppressWarnings("unchecked") T element = (T) rawElement;
					synchronized (list) {
						if (modification.action == Action.ADD) {
							if (modification.index >= 0) {
								list.add(modification.index, element);
								myList.add(modification.index, element);
							} else {
								list.add(element);
								myList.add(element);
							}
						} else if (modification.action == Action.REPLACE) {
							if (modification.index < myList.size()) {
								myList.set(modification.index, element);
								list.set(modification.index, element);
							} else System.out.println("A replacement failed");
						} else throw new Error("Unexpected action " + modification.action);
					}
				});
			}
		}
	}

	@Override
	Object getState() {
		return list;
	}

	@BitEnum(mode = BitEnum.Mode.UniformOrdinal)
	private enum Action {
		ADD,
		REPLACE,
		REMOVE
	}

	@BitStruct(backwardCompatible = false)
	private static class Modification<T> {

		// This is not an @BitField because we don't know its type
		final T element;

		@BitField(ordering = 0)
		@IntegerField(expectUniform = false, minValue = -1)
		final int index;

		@BitField(ordering = 1)
		final Action action;

		Modification(T element, int index, Action action) {
			this.element = element;
			this.index = index;
			this.action = action;
		}

		@SuppressWarnings("unused")
		Modification() {
			this(null, 0, Action.ADD);
		}
	}
}
