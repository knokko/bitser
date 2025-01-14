package com.github.knokko.bitser.connection;

import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.serialize.Bitser;
import com.github.knokko.bitser.wrapper.BitserWrapper;

import java.io.IOException;
import java.util.function.Consumer;

public class BitStructConnection<T> {

	private final Bitser bitser;
	private final BitserWrapper<T> wrapper;
	private T referenceState;
	public final T state;
	private final Consumer<ChangeListener> reportChanges;

	public BitStructConnection(Bitser bitser, T state, Consumer<ChangeListener> reportChanges) {
		this.bitser = bitser;
		@SuppressWarnings("unchecked") Class<T> structClass = (Class<T>) state.getClass();
		this.wrapper = bitser.cache.getWrapper(structClass);
		this.referenceState = wrapper.shallowCopy(state);
		this.state = state;
		this.reportChanges = reportChanges;
	}

	public void checkForChanges() {
		synchronized (state) {
			// TODO What about the with?
			try {

				if (wrapper.findAndWriteChanges(bitser, null, referenceState, state) == 0) return;
			} catch (IOException shouldNotHappen) {
				throw new RuntimeException(shouldNotHappen);
			}
			reportChanges.accept(output -> wrapper.findAndWriteChanges(bitser, output, referenceState, state));
			referenceState = wrapper.shallowCopy(state);
		}
	}

	public void handleChanges(BitInputStream input) throws IOException {
		synchronized (state) {
			// TODO What about the with?
			wrapper.readAndApplyChanges(bitser, input, state);
			referenceState = wrapper.shallowCopy(state);
		}
	}

	@FunctionalInterface
	public interface ChangeListener {
		void report(BitOutputStream output) throws IOException;
	}
}
