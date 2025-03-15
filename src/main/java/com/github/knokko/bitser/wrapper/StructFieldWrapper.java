package com.github.knokko.bitser.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.backward.LegacyClasses;
import com.github.knokko.bitser.backward.instance.LegacyStructInstance;
import com.github.knokko.bitser.backward.LegacyStruct;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.field.*;
import com.github.knokko.bitser.serialize.BitPostInit;
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
import java.util.function.Consumer;

import static com.github.knokko.bitser.serialize.IntegerBitser.decodeUniformInteger;
import static com.github.knokko.bitser.serialize.IntegerBitser.encodeUniformInteger;

@BitStruct(backwardCompatible = false)
public class StructFieldWrapper extends BitFieldWrapper implements BitPostInit {

	private final Class<?>[] allowed;

	private LegacyStruct[] legacyStructs;

	@SuppressWarnings("unused")
	@BitField(id = 0)
	@NestedFieldSetting(path = "c", optional = true)
	@ReferenceField(stable = false, label = "structs")
	private LegacyStruct[] legacyStructs(FunctionContext context) {
		LegacyClasses legacyClasses = (LegacyClasses) context.withParameters.get("legacy-classes");
		LegacyStruct[] allowedStructs = new LegacyStruct[allowed.length];
		for (int index = 0; index < allowed.length; index++) {
			allowedStructs[index] = legacyClasses.getStruct(allowed[index]);
		}
		return allowedStructs;
	}

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
	public void postInit(BitPostInit.Context context) {
		this.legacyStructs = (LegacyStruct[]) context.functionValues.get(StructFieldWrapper.class)[0];
		//for (LegacyStruct legacy : legacyStructs) Objects.requireNonNull(legacy);
	}

	@Override
	public void collectReferenceLabels(LabelCollection labels) {
		super.collectReferenceLabels(labels);
		if (allowed.length == 0) {
			for (LegacyStruct legacy : legacyStructs) {
				if (legacy != null) legacy.collectReferenceLabels(labels);
			}
		}
		for (Class<?> structClass : allowed) {
			labels.cache.getWrapper(structClass).collectReferenceLabels(labels);
		}
	}

	@Override
	public void collectUsedReferenceLabels(LabelCollection labels, Object value) {
		super.collectUsedReferenceLabels(labels, value);
		if (value != null) {
			labels.cache.getWrapper(value.getClass()).collectUsedReferenceLabels(labels, value);
		}
	}

	@Override
	void registerReferenceTargets(Object value, BitserCache cache, ReferenceIdMapper idMapper) {
		super.registerReferenceTargets(value, cache, idMapper);
		if (value != null) cache.getWrapper(value.getClass()).registerReferenceTargets(value, cache, idMapper);
	}

	@Override
	void registerLegacyClasses(Object value, LegacyClasses legacy) {
		super.registerLegacyClasses(value, legacy);
		if (value == null) return;
		legacy.cache.getWrapper(value.getClass()).registerClasses(value, legacy);
	}

	@Override
	void writeValue(Object value, WriteJob write) throws IOException {
		for (int index = 0; index < allowed.length; index++) {
			if (allowed[index] == value.getClass())	{
				encodeUniformInteger(index, 0, allowed.length - 1, write.output);
				write.bitser.cache.getWrapper(value.getClass()).write(value, write);
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
		int inheritanceIndex = (int) decodeUniformInteger(0, length - 1, read.input);

		if (allowed.length == 0) {
			legacyStructs[inheritanceIndex].read(read, inheritanceIndex, setValue::consume);
		} else read.bitser.cache.getWrapper(allowed[inheritanceIndex]).read(read, setValue);
	}

	@Override
	void setLegacyValue(ReadJob read, Object value, Consumer<Object> setValue) {
		if (value == null) {
			super.setLegacyValue(read, null, setValue);
			return;
		}
		LegacyStructInstance legacy = (LegacyStructInstance) value;
		BitserWrapper<?> valueWrapper = read.bitser.cache.getWrapper(allowed[legacy.inheritanceIndex]);
		setValue.accept(valueWrapper.setLegacyValues(read, legacy));
	}

	@Override
	public void fixLegacyTypes(ReadJob read, Object value) {
		if (value == null && field.optional) return;
		LegacyStructInstance instance = (LegacyStructInstance) value;
		if (instance.inheritanceIndex >= allowed.length) throw new InvalidBitValueException(
				"Encountered unknown subclass while loading " + field
		);
		read.bitser.cache.getWrapper(allowed[instance.inheritanceIndex]).fixLegacyTypes(read, instance);
		if (field.referenceTargetLabel != null) {
			read.idLoader.replace(field.referenceTargetLabel, instance, instance.newInstance);
		}
	}
}
