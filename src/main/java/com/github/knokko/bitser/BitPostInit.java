package com.github.knokko.bitser;

import java.util.Map;
import java.util.Objects;

public interface BitPostInit {

	void postInit(Context context);

	class Context {

		public final Bitser bitser;
		public final boolean backwardCompatible;

		/**
		 * Will be {@code null} if and only if {@link #backwardCompatible} is {@code true} and this struct (instance)
		 * did <i>not</i> yet exist when it was serialized, for instance when:
		 * <ol>
		 *     <li>Some bit struct {@code OuterStruct} is saved to disk</li>
		 *     <li>You create a new class {@code InnerStruct} that implements {@link BitPostInit}</li>
		 *     <li>You add a new field of type {@code InnerStruct} to {@code OuterStruct}</li>
		 *     <li>You deserialize an {@code OuterStruct} from the file saved in step 1</li>
		 *     <li>
		 *         The {@code functionValues} of the {@code Context} passed to {@code InnerStruct.postInit}
		 *         will be {@code null}, since that {@code InnerStruct} was never saved.
		 *     </li>
		 * </ol>
		 * Note that in this example, steps 1 and 2 could be swapped: it only matters that step 3 comes after step 1,
		 * and that step 4 comes after step 3.
		 */
		// TODO Add more docs than just when it's null
		public final Map<Class<?>, Object[]> functionValues;

		/**
		 * Will be {@code null} if and only if {@link #backwardCompatible} is {@code false}
		 */
		public final Map<Class<?>, Object[]> legacyFieldValues;

		/**
		 * Will be {@code null} if and only if {@link #backwardCompatible} is {@code false}
		 */
		public final Map<Class<?>, Object[]> legacyFunctionValues;
		public final Map<String, Object> withParameters;

		public Context(
				Bitser bitser,
				boolean backwardCompatible,
				Map<Class<?>, Object[]> functionValues,
				Map<Class<?>, Object[]> legacyFieldValues,
				Map<Class<?>, Object[]> legacyFunctionValues,
				Map<String, Object> withParameters
		) {
			this.bitser = Objects.requireNonNull(bitser);
			this.backwardCompatible = backwardCompatible;
			this.functionValues = functionValues;
			this.legacyFieldValues = legacyFieldValues;
			this.legacyFunctionValues = legacyFunctionValues;
			this.withParameters = Objects.requireNonNull(withParameters);
		}
	}
}
