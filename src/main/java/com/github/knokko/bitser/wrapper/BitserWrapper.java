package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.backward.LegacyClasses;
import com.github.knokko.bitser.backward.instance.LegacyStructInstance;
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

	public abstract void collectReferenceLabels(LabelCollection labels);

	public abstract void collectUsedReferenceLabels(LabelCollection labels, Object value);

	public abstract void registerReferenceTargets(
			Object object, BitserCache cache, ReferenceIdMapper idMapper
	);

	public abstract LegacyStruct registerClasses(Object object, LegacyClasses legacy);

	public abstract UUID getStableId(Object target);

	public abstract void write(Object object, WriteJob write) throws IOException;

	public abstract void read(ReadJob read, ValueConsumer setValue) throws IOException;

	public abstract T setLegacyValues(ReadJob read, LegacyStructInstance legacy);

	public abstract void fixLegacyTypes(ReadJob read, LegacyStructInstance legacyInstance);

	public abstract T shallowCopy(Object original);

	public abstract <C> BitStructConnection<C> createConnection(
			Bitser bitser, C object, Consumer<BitStructConnection.ChangeListener> reportChanges
	);
}
