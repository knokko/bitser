package com.github.knokko.bitser.legacy;

import java.util.Objects;

/**
 * Instances of this class are created during the backward-compatible deserialization of a
 * {@link com.github.knokko.bitser.SimpleLazyBits} field. This class is usually not interesting for users, but it may
 * appear in the legacy values of a {@link com.github.knokko.bitser.BitPostInit.Context}.
 */
public class LegacyLazyBytes {

	/**
	 * The bytes containing the data of the wrapped struct that was serialized. You should be able to deserialize it
	 * using {@link com.github.knokko.bitser.Bitser#deserializeFromBytes}.
	 */
	public final byte[] bytes;

	/**
	 * This constructor is meant for internal use only.
	 */
	public LegacyLazyBytes(byte[] bytes) {
		this.bytes = Objects.requireNonNull(bytes);
	}
}
