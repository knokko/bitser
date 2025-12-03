package com.github.knokko.bitser.serialize;

/**
 * <p>
 *   Instances of this class can be added to the <i>withAndOptions</i> varargs of {@link Bitser#deserialize}.
 *   When added, the {@link #maxSize} will limit the maximum size/length of arrays, collections, and strings that
 *   bitser will create during deserialization.
 * </p>
 *
 * <p>
 *   This can be used to defend against denial-of-service attacks when you deserialize an untrusted byte sequence: if an
 *   attacker would specify a ridiculously large size of a collection, bitser will attempt to allocate memory for it,
 *   which could reduce the memory available for the rest of the application. When a <i>CollectionSizeLimit</i> is
 *   present, bitser will throw an {@link com.github.knokko.bitser.exceptions.InvalidBitValueException} when the size
 *   of a deserialized collection is too large, instead of allocating memory for it.
 * </p>
 *
 * <p>
 *   Note that this also limits the maximum size that your legitimate collections can have, so you should not set this
 *   limit to something too small.
 * </p>
 */
public class CollectionSizeLimit {

	/**
	 * The maximum size (number of elements or characters) that deserialized arrays, collections, and strings can
	 * have.
	 */
	public final int maxSize;

	/**
	 * See {@link CollectionSizeLimit}
	 */
	public CollectionSizeLimit(int maxSize) {
		this.maxSize = maxSize;
	}
}
