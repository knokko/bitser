package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.UnexpectedBitserException;
import com.github.knokko.bitser.legacy.LegacyLazyBytes;
import com.github.knokko.bitser.legacy.LegacyStructInstance;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.exceptions.LegacyBitserException;
import com.github.knokko.bitser.field.*;
import com.github.knokko.bitser.util.JobOutput;
import com.github.knokko.bitser.util.Recursor;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.function.Consumer;

import static com.github.knokko.bitser.IntegerBitser.decodeUniformInteger;
import static com.github.knokko.bitser.IntegerBitser.encodeUniformInteger;

@BitStruct(backwardCompatible = false)
class StructFieldWrapper extends BitFieldWrapper implements BitPostInit {

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
	}

	@Override
	void collectReferenceLabels(Recursor<LabelContext, LabelInfo> recursor) {
		super.collectReferenceLabels(recursor);
		if (allowed.length == 0) {
			for (LegacyStruct legacy : legacyStructs) {
				if (legacy != null) legacy.collectReferenceLabels(recursor);
			}
		}
		for (Class<?> structClass : allowed) {
			recursor.info.cache.getWrapper(structClass).collectReferenceLabels(recursor);
		}
	}

	@Override
	void registerReferenceTargets(Object value, Recursor<ReferenceIdMapper, BitserCache> recursor) {
		super.registerReferenceTargets(value, recursor);
		if (value != null) {
			// No need to call recursor.nested, since BitStructWrapper will do that anyway
			recursor.info.getWrapper(value.getClass()).registerReferenceTargets(value, recursor);
		}
	}

	@Override
	void registerLegacyClasses(Object value, Recursor<LegacyClasses, LegacyInfo> recursor) {
		super.registerLegacyClasses(value, recursor);
		if (value == null) return;
		recursor.info.cache.getWrapper(value.getClass()).registerClasses(value, recursor);
	}

	@Override
	void writeValue(Object value, Recursor<WriteContext, WriteInfo> recursor) {
		for (int index = 0; index < allowed.length; index++) {
			if (allowed[index] == value.getClass())	{
				final int rememberIndex = index;
				if (allowed.length > 1) {
					recursor.runFlat("allowed-class-index", context -> {
						context.output.prepareProperty("allowed-class-index", -1);
						encodeUniformInteger(rememberIndex, 0, allowed.length - 1, context.output);
						context.output.finishProperty();
					});
				}

				recursor.info.bitser.cache.getWrapper(value.getClass()).write(value, recursor);
				return;
			}
		}

		throw new InvalidBitValueException(
				"Struct class " + value.getClass() + " is not in " + Arrays.toString(allowed)
		);
	}

	@Override
	void readValue(Recursor<ReadContext, ReadInfo> recursor, Consumer<Object> setValue) {
		int length = allowed.length == 0 ? legacyStructs.length : allowed.length;
		if (length > 1) {
			JobOutput<Integer> inheritanceIndex = recursor.computeFlat("inheritance-index", context ->
					(int) decodeUniformInteger(0, length - 1, context.input)
			);

			recursor.runNested("struct", nested -> {
				if (allowed.length == 0) {
					legacyStructs[inheritanceIndex.get()].read(nested, inheritanceIndex.get(), setValue::accept);
				} else nested.info.bitser.cache.getWrapper(allowed[inheritanceIndex.get()]).read(nested, setValue);
			});
		} else {
			if (allowed.length == 0) {
				legacyStructs[0].read(recursor, 0, setValue::accept);
			} else recursor.info.bitser.cache.getWrapper(allowed[0]).read(recursor, setValue);
		}
	}

	@Override
	public void write(
			Serializer serializer, Object value,
			RecursionNode parentNode, String fieldName
	) throws Throwable {
		for (int index = 0; index < allowed.length; index++) {
			if (allowed[index] == value.getClass())	{
				serializer.output.prepareProperty("allowed-class-index", -1);
				encodeUniformInteger(index, 0, allowed.length - 1, serializer.output);
				serializer.output.finishProperty();

				serializer.structJobs.add(new WriteStructJob(
						value, serializer.cache.getWrapper(value.getClass()),
						new RecursionNode(parentNode, fieldName)
				));
				return;
			}
		}

		throw new InvalidBitValueException(
				"Struct class " + value.getClass() + " is not in " + Arrays.toString(allowed)
		);
	}

	@Override
	public Object read(Deserializer deserializer, RecursionNode parentNode, String fieldName) throws Throwable {
		int length = allowed.length == 0 ? legacyStructs.length : allowed.length;
		int inheritanceIndex = (int) decodeUniformInteger(0, length - 1, deserializer.input);
		if (allowed.length == 0) {
			// TODO Backward compatibility
			throw new UnsupportedOperationException("TODO");
		} else {
			BitStructWrapper<?> structInfo = deserializer.cache.getWrapper(allowed[inheritanceIndex]);
			Object structObject = structInfo.createEmptyInstance();
			deserializer.structJobs.add(
					new ReadStructJob(structObject, structInfo, new RecursionNode(parentNode, fieldName))
			);
			return structObject;
		}
	}

	@Override
	void setLegacyValue(Recursor<ReadContext, ReadInfo> recursor, Object value, Consumer<Object> setValue) {
		if (value == null) {
			super.setLegacyValue(recursor, null, setValue);
			return;
		}
		if (value instanceof LegacyLazyBytes) {
			if (allowed.length != 1) throw new UnexpectedBitserException(
					"LegacyLazyBytes should have been denied at fixLegacyTypes"
			);
			setValue.accept(recursor.info.bitser.deserializeFromBytes(
					allowed[0], ((LegacyLazyBytes) value).bytes, Bitser.BACKWARD_COMPATIBLE
			));
			return;
		}
		LegacyStructInstance legacy = (LegacyStructInstance) value;
		BitStructWrapper<?> valueWrapper = recursor.info.bitser.cache.getWrapper(allowed[legacy.inheritanceIndex]);
		setValue.accept(valueWrapper.setLegacyValues(recursor, legacy));
	}

	@Override
	void fixLegacyTypes(Recursor<ReadContext, ReadInfo> recursor, Object value) {
		if (value == null && field.optional) return;
		if (value instanceof LegacyLazyBytes && allowed.length == 1) return;
		if (!(value instanceof LegacyStructInstance)) {
			throw new LegacyBitserException("Can't convert from legacy " + value + " to a BitStruct");
		}

		LegacyStructInstance instance = (LegacyStructInstance) value;
		if (instance.inheritanceIndex >= allowed.length) throw new LegacyBitserException(
				"Encountered unknown subclass while loading " + field
		);
		recursor.info.bitser.cache.getWrapper(allowed[instance.inheritanceIndex]).fixLegacyTypes(recursor, instance);
		if (field.referenceTargetLabel != null) {
			recursor.runFlat("referenceTargetLabel", context ->
					context.idLoader.replace(field.referenceTargetLabel, instance, instance.newInstance)
			);
		}
	}

	@Override
	boolean deepEquals(Object a, Object b, BitserCache cache) {
		if (a == null && b == null) return true;
		if (a == null || b == null) return false;
		BitStructWrapper<?> wrapperA = cache.getWrapper(a.getClass());
		BitStructWrapper<?> wrapperB = cache.getWrapper(b.getClass());
		return wrapperA == wrapperB && wrapperA.deepEquals(a, b, cache);
	}

	@Override
	int hashCode(Object value, BitserCache cache) {
		if (value == null) return 17;
		return cache.getWrapper(value.getClass()).hashCode(value, cache);
	}
}
