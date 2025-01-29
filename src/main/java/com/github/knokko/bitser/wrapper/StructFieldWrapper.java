package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.backward.LegacyClasses;
import com.github.knokko.bitser.backward.LegacyStruct;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.field.ClassField;
import com.github.knokko.bitser.field.ReferenceField;
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

	private final Class<?>[] allowed;

	@ReferenceField(stable = false, label = "structs")
	private LegacyStruct[] legacyStructs;

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
	public void collectReferenceTargetLabels(LabelCollection labels) {
		super.collectReferenceTargetLabels(labels);

		if (allowed.length == 0) {
			for (LegacyStruct legacy : legacyStructs) {
				legacy.collectReferenceTargetLabels(labels);
			}
		} else {
			for (Class<?> structClass : allowed) {
				labels.cache.getWrapper(structClass).collectReferenceTargetLabels(labels);
			}
		}
	}

	@Override
	void registerReferenceTargets(Object value, BitserCache cache, ReferenceIdMapper idMapper) {
		super.registerReferenceTargets(value, cache, idMapper);
		// TODO maybe override
		if (value != null) cache.getWrapper(value.getClass()).registerReferenceTargets(value, cache, idMapper);
	}

	@Override
	void registerLegacyClasses(LegacyClasses legacy) {
		// TODO Find a nicer way to handle this...
		if (legacyStructs == null) legacyStructs = new LegacyStruct[allowed.length];
		for (int index = 0; index < allowed.length; index++) {
			// TODO Maybe only register the classes that are actually used?
			legacyStructs[index] = legacy.cache.getWrapper(allowed[index]).registerClasses(legacy);
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
		int length = allowed.length == 0 ? legacyStructs.length : allowed.length;
		int index = (int) decodeUniformInteger(0, length - 1, read.input);


		if (allowed.length == 0) {
			System.out.println("StructFieldWrapper.readValue...");
//			read.idLoader.getUnstable("structs", element -> {
//				System.out.println("bla bla");
//			}, read.input);
			legacyStructs[index].read(read, setValue);
		}
		else read.cache.getWrapper(allowed[index]).read(read, setValue, null);
	}

	@Override
	void setLegacyValue(ReadJob read, Object target, Object value) {
		// TODO Figure out the right index
		int index = 0;
		field.setValue.accept(target, read.cache.getWrapper(allowed[index]).setLegacyValues(read, value));
	}
}
