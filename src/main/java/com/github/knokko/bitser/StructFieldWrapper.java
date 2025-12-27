package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.UnexpectedBitserException;
import com.github.knokko.bitser.legacy.LegacyStructInstance;
import com.github.knokko.bitser.legacy.LegacyLazyBytes;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.exceptions.LegacyBitserException;
import com.github.knokko.bitser.field.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;

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
	void registerLegacyClasses(UsedStructCollector collector) {
		for (Class<?> allowedStructClass : allowed) collector.maybeRegisterStruct(allowedStructClass);
	}

	@Override
	void registerReferenceTargets(
			AbstractReferenceTracker references, Object value,
			RecursionNode parentNode, String fieldName
	) {
		BitStructWrapper<?> structInfo = references.cache.getWrapper(value.getClass());
		references.structJobs.add(new WithStructJob(value, structInfo, new RecursionNode(parentNode, fieldName)));
	}

	@Override
	public void write(
			Serializer serializer, Object value,
			RecursionNode parentNode, String fieldName
	) throws Throwable {
		for (int index = 0; index < allowed.length; index++) {
			if (allowed[index] == value.getClass())	{
				if (allowed.length > 1) {
					serializer.output.prepareProperty("allowed-class-index", -1);
					encodeUniformInteger(index, 0, allowed.length - 1, serializer.output);
					serializer.output.finishProperty();
				}

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
		int allowedClassIndex;
		if (allowed.length > 1) {
			deserializer.input.prepareProperty("allowed-class-index", -1);
			allowedClassIndex = (int) decodeUniformInteger(0, allowed.length - 1, deserializer.input);
			deserializer.input.finishProperty();
		} else allowedClassIndex = 0;

		BitStructWrapper<?> structInfo = deserializer.cache.getWrapper(allowed[allowedClassIndex]);
		Object structObject = structInfo.createEmptyInstance();
		deserializer.structJobs.add(
				new ReadStructJob(structObject, structInfo, new RecursionNode(parentNode, fieldName))
		);
		return structObject;
	}

	@Override
	Object read(BackDeserializer deserializer, RecursionNode parentNode, String fieldName) throws Throwable {
		int allowedClassIndex;
		if (legacyStructs.length > 1) {
			deserializer.input.prepareProperty("allowed-class-index", -1);
			allowedClassIndex = (int) decodeUniformInteger(0, legacyStructs.length - 1, deserializer.input);
			deserializer.input.finishProperty();
		} else allowedClassIndex = 0;

		LegacyStruct legacyInfo = legacyStructs[allowedClassIndex];
		LegacyStructInstance legacyObject = legacyInfo.constructEmptyInstance(allowedClassIndex);
		deserializer.structJobs.add(new BackReadStructJob(
				legacyObject, legacyInfo,
				new RecursionNode(parentNode, fieldName)
		));
		return legacyObject;
	}

	@Override
	Object convert(BackDeserializer deserializer, Object legacyValue, RecursionNode parentNode, String fieldName) {
		if (legacyValue instanceof LegacyLazyBytes) {
			if (allowed.length != 1) throw new UnexpectedBitserException(
					"LegacyLazyBytes should have been denied at fixLegacyTypes"
			);
			return deserializer.bitser.fromBytes(
					allowed[0], ((LegacyLazyBytes) legacyValue).bytes(), Bitser.BACKWARD_COMPATIBLE
			);
		}
		if (legacyValue instanceof LegacyStructInstance) {
			LegacyStructInstance legacyObject = (LegacyStructInstance) legacyValue;
			if (legacyObject.allowedClassIndex >= allowed.length) throw new LegacyBitserException(
					"Encountered unknown subclass while loading " + field
			);
			BitStructWrapper<?> modernInfo = deserializer.bitser.cache.getWrapper(allowed[legacyObject.allowedClassIndex]);
			Object modernObject = modernInfo.createEmptyInstance();
			deserializer.convertStructJobs.add(new BackConvertStructJob(
					modernObject, modernInfo, legacyObject,
					new RecursionNode(parentNode, fieldName)
			));
			return modernObject;
		}
		throw new LegacyBitserException(
				"Can't convert from legacy " + legacyValue + " to " + field.type + " for field " + field
		);
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
