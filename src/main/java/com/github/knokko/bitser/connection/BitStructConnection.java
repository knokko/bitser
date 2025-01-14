package com.github.knokko.bitser.connection;

import com.github.knokko.bitser.serialize.Bitser;
import com.github.knokko.bitser.wrapper.BitserWrapper;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

public class BitStructConnection<T> {

	private final Bitser bitser;
	private final BitserWrapper<T> wrapper;
	private T referenceState;
	public final T state;
	private final Consumer<List<BitStructChange>> reportChanges;

	public BitStructConnection(Bitser bitser, T state, Consumer<List<BitStructChange>> reportChanges) {
		this.bitser = bitser;
		@SuppressWarnings("unchecked") Class<T> structClass = (Class<T>) state.getClass();
		this.wrapper = bitser.cache.getWrapper(structClass);
		this.referenceState = wrapper.shallowCopy(state);
		this.state = state;
		this.reportChanges = reportChanges;
	}

	public void checkForChanges() {
		List<BitStructChange> changes;
		synchronized (state) {
			changes = wrapper.findChanges(bitser, referenceState, state); // TODO What about the with?
			referenceState = wrapper.shallowCopy(state);
		}

		reportChanges.accept(changes);
	}

	public void handleChanges(List<BitStructChange> changes) throws IOException {
		synchronized (state) {
			// TODO Maybe merge the changes
			for (BitStructChange change : changes) wrapper.handleChange(state, change, bitser.cache);
		}
	}
}
