package com.github.knokko.bitser;

import java.util.Map;
import java.util.Objects;

public interface BitPostInit {

	void postInit(Context context);

	class Context {

		public final Bitser bitser;
		public final boolean backwardCompatible;
		public final Map<Class<?>, Object[]> functionValues;
		public final Map<Class<?>, Object[]> legacyFieldValues;
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
			this.withParameters = withParameters;
		}
	}
}
