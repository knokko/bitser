package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.backward.LegacyClasses;
import com.github.knokko.bitser.backward.LegacyInstance;
import com.github.knokko.bitser.backward.LegacyStruct;
import com.github.knokko.bitser.connection.BitStructConnection;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.serialize.*;
import com.github.knokko.bitser.util.ReferenceIdMapper;

import java.io.IOException;
import java.util.UUID;
import java.util.function.Consumer;

public abstract class BitserWrapper<T> {

	public static <T> BitserWrapper<T> wrap(Class<T> objectClass) {
		BitStruct bitStruct = objectClass.getAnnotation(BitStruct.class);
		if (bitStruct != null) return new BitStructWrapper<>(objectClass, bitStruct);

		throw new InvalidBitFieldException(objectClass + " is not a BitStruct");
	}

	BitserWrapper() {}

	public abstract void collectReferenceTargetLabels(LabelCollection labels);

	public abstract void registerReferenceTargets(
			Object object, BitserCache cache, ReferenceIdMapper idMapper
	);

	public abstract LegacyStruct registerClasses(Object object, LegacyClasses legacy);

	public abstract UUID getStableId(Object target);

	public abstract void write(Object object, WriteJob write) throws IOException;

	public abstract void read(ReadJob read, ValueConsumer setValue, LegacyStruct legacyStruct) throws IOException;

	public abstract T setLegacyValues(ReadJob read, LegacyInstance legacy);

	public abstract T shallowCopy(Object original);

	public abstract <C> BitStructConnection<C> createConnection(
			Bitser bitser, C object, Consumer<BitStructConnection.ChangeListener> reportChanges
	);
}
