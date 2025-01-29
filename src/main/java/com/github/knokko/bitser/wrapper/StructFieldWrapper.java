package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.backward.LegacyClasses;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.field.ClassField;
import com.github.knokko.bitser.serialize.BitserCache;
import com.github.knokko.bitser.serialize.LabelCollection;
import com.github.knokko.bitser.serialize.ReadJob;
import com.github.knokko.bitser.serialize.WriteJob;
import com.github.knokko.bitser.util.ReferenceIdMapper;
import com.github.knokko.bitser.util.VirtualField;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import static com.github.knokko.bitser.serialize.IntegerBitser.decodeUniformInteger;
import static com.github.knokko.bitser.serialize.IntegerBitser.encodeUniformInteger;

@BitStruct(backwardCompatible = false)
public class StructFieldWrapper extends BitFieldWrapper {

	// TODO How do I deserialize this?
	private final Class<?>[] allowed;

	StructFieldWrapper(VirtualField field, ClassField classField) {
		super(field);
		if (classField != null) {
			try {
				Field hierarchyField = classField.root().getDeclaredField("BITSER_HIERARCHY");
				if (!Modifier.isPublic(hierarchyField.getModifiers())) hierarchyField.setAccessible(true);
				allowed = (Class<?>[]) hierarchyField.get(null);
			} catch (NoSuchFieldException e) {
				throw new InvalidBitFieldException("Class " + classField.root() + " must have a constant named 'BITSER_HIERARCHY'");
			} catch (ClassCastException wrongType) {
				throw new InvalidBitFieldException("BITSER_HIERARCHY of " + classField.root() + " must be a Class<?>[]");
			} catch (IllegalAccessException e) {
				throw new InvalidBitFieldException("Can't make BITSER_HIERARCHY of " + classField.root() + " accessible");
			}
		} else this.allowed = new Class<?>[] { field.type };
	}

	@SuppressWarnings("unused")
	private StructFieldWrapper() {
		super();
		this.allowed = new Class[]{};
	}

	@Override
	void collectReferenceTargetLabels(LabelCollection labels) {
		super.collectReferenceTargetLabels(labels);

		for (Class<?> structClass : allowed) {
			labels.cache.getWrapper(structClass).collectReferenceTargetLabels(labels);
		}
	}

	@Override
	void registerReferenceTargets(Object value, BitserCache cache, ReferenceIdMapper idMapper) {
		super.registerReferenceTargets(value, cache, idMapper);
		if (value != null) cache.getWrapper(value.getClass()).registerReferenceTargets(value, cache, idMapper);
	}

	@Override
	void registerLegacyClasses(LegacyClasses legacy) {
		for (Class<?> structClass : allowed) {
			// TODO Maybe only register the classes that are actually used?
			legacy.cache.getWrapper(structClass).registerClasses(legacy);
		}
	}

	@Override
	void writeValue(Object value, WriteJob write) throws IOException {
		for (int index = 0; index < allowed.length; index++) {
			if (allowed[index] == value.getClass())	{
				encodeUniformInteger(index, 0, allowed.length - 1, write.output);
				write.cache.getWrapper(value.getClass()).write(value, write);
				return;
			}
		}

		throw new InvalidBitValueException(
				"Struct class " + value.getClass() + " is not in " + Arrays.toString(allowed)
		);
	}

	@Override
	void readValue(ReadJob read, ValueConsumer setValue) throws IOException {
		int index = (int) decodeUniformInteger(0, allowed.length - 1, read.input);
		read.cache.getWrapper(allowed[index]).read(read, setValue);
	}
}
