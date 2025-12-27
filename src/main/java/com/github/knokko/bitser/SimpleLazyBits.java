package com.github.knokko.bitser;

import java.util.Objects;

/**
 * A wrapper around a {@link BitStruct} that is (de)serialized <i>lazily</i>. You can wrap a <i>BitStruct</i> instance
 * using the public constructor: {@code SimpleLazyBits<YourStruct> lazy = new SimpleLazyBits<>(structInstance);}
 * and you can get access to the original {@code structInstance} by calling {@code lazy.get()}.
 * <ol>
 *     <li>
 *         When a <i>SimpleLazyBits</i> field is 'deserialized', its raw byte data will be taken from the stream,
 *         but it will <i>not</i> be decoded/deserialized, which potentially saves lots of time,
 *         since deserializing big objects takes much more time than copying/moving bytes.
 *     </li>
 *     <li>
 *         When you call the {@link #get()} method of a 'deserialized' <i>SimpleLazyBits</i> for the first time, the
 *         wrapped value will be deserialized (which may take some time). All subsequent invocations will be quick.
 *     </li>
 *     <li>
 *         When a deserialized <i>SimpleLazyBits</i> field is serialized again, the raw byte data will be sent to the
 *         output stream right away. There is no need to actually serialize the value again, since the serialized bytes
 *         are already known.
 *     </li>
 * </ol>
 * This class is potentially useful for wrapping big struct classes that are probably not going to be needed right
 * away. Note that instances of <i>SimpleLazyBits</i> are immutable, so you can only 'modify' them by replacing them
 * with another instance of <i>SimpleLazyBits</i>.
 * @param <T> The type of the values to be wrapped. The class must be annotated with {@link BitStruct}.
 */
public final class SimpleLazyBits<T> {

	private static final Object[] NOT_BACKWARD_COMPATIBLE = new Object[0];
	private static final Object[] BACKWARD_COMPATIBLE = { Bitser.BACKWARD_COMPATIBLE };

	private Class<? extends T> valueClass;
	private Bitser bitser;
	private boolean backwardCompatible;

	private T value;
	byte[] bytes;

	/**
	 * Creates a new {@link SimpleLazyBits} that wraps {@code value}. You can access it later by calling {@link #get()}.
	 */
	public SimpleLazyBits(T value) {
		this.value = Objects.requireNonNull(value);
	}

	SimpleLazyBits(byte[] bytes, Bitser bitser, boolean backwardCompatible, Class<? extends T> valueClass) {
		this.value = null;
		this.bytes = Objects.requireNonNull(bytes);
		this.bitser = Objects.requireNonNull(bitser);
		this.backwardCompatible = backwardCompatible;
		this.valueClass = Objects.requireNonNull(valueClass);
	}

	/**
	 * Gets the wrapped value that was supplied to the constructor. Calling this method is usually very cheap, except
	 * when it is called for the first time on a deserialized instance of this class.
	 */
	public T get() {
		if (value == null) {
			Object[] options = backwardCompatible ? BACKWARD_COMPATIBLE : NOT_BACKWARD_COMPATIBLE;
			value = bitser.fromBytes(valueClass, bytes, options);
		}
		return value;
	}
}
