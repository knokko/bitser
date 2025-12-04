package com.github.knokko.bitser;

import com.github.knokko.bitser.legacy.LegacyCollectionInstance;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.field.ClassField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.util.JobOutput;
import com.github.knokko.bitser.util.Recursor;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

@BitStruct(backwardCompatible = false)
class BitCollectionFieldWrapper extends AbstractCollectionFieldWrapper {

	@ClassField(root = BitFieldWrapper.class)
	private final BitFieldWrapper valuesWrapper;

	BitCollectionFieldWrapper(VirtualField field, IntegerField sizeField, BitFieldWrapper valuesWrapper) {
		super(field, sizeField);
		this.valuesWrapper = valuesWrapper;
	}

	@SuppressWarnings("unused")
	private BitCollectionFieldWrapper() {
		super();
		this.valuesWrapper = null;
	}

	@Override
	ArrayType determineArrayType() {
		return null;
	}

	@Override
	void registerLegacyClasses(Object value, Recursor<LegacyClasses, LegacyInfo> recursor) {
		super.registerLegacyClasses(value, recursor);
		if (value == null) return;
		if (value.getClass().isArray()) {
			int size = Array.getLength(value);
			for (int index = 0; index < size; index++) {
				Object element = Array.get(value, index);
				recursor.runNested("element " + index, nested ->
						valuesWrapper.registerLegacyClasses(element, nested)
				);
			}
		} else {
			int counter = 0;
			for (Object element : (Collection<?>) value) {
				recursor.runNested("element " + counter, nested ->
						valuesWrapper.registerLegacyClasses(element, nested)
				);
				counter += 1;
			}
		}
	}

	@Override
	BitFieldWrapper getChildWrapper() {
		return valuesWrapper;
	}

	@Override
	void collectReferenceLabels(Recursor<LabelContext, LabelInfo> recursor) {
		super.collectReferenceLabels(recursor);
		recursor.runNested("values", valuesWrapper::collectReferenceLabels);
	}

	@Override
	void registerReferenceTargets(Object value, Recursor<ReferenceIdMapper, BitserCache> recursor) {
		super.registerReferenceTargets(value, recursor);
		if (value == null) return;
		if (field.type.isArray()) {
			int size = Array.getLength(value);
			for (int index = 0; index < size; index++) {
				Object element = Array.get(value, index);
				recursor.runNested("element " + index, nested ->
						valuesWrapper.registerReferenceTargets(element, nested)
				);
			}
		} else {
			int index = 0;
			for (Object element : (Collection<?>) value) {
				recursor.runNested("element " + index, nested ->
						valuesWrapper.registerReferenceTargets(element, nested)
				);
				index += 1;
			}
		}
	}

	@Override
	void writeElements(Object value, int size, Recursor<WriteContext, WriteInfo> recursor) {
		String nullErrorMessage = "Field " + field + " must not have null elements";
		if (field.type.isArray()) {
			for (int index = 0; index < size; index++) {
				final int rememberIndex = index;
				recursor.runFlat("pushContext", context ->
						context.output.pushContext("element", rememberIndex)
				);
				recursor.runNested("element " + index, nested ->
						writeElement(Array.get(value, rememberIndex), valuesWrapper, nested, nullErrorMessage)
				);
				recursor.runFlat("popContext", context ->
						context.output.popContext("element", rememberIndex)
				);
			}
		} else {
			int counter = 0;
			for (Object element : (Collection<?>) value) {
				final int rememberCounter = counter;
				recursor.runFlat("pushContext", context ->
						context.output.pushContext("element", rememberCounter)
				);
				recursor.runNested("element " + counter, nested ->
						writeElement(element, valuesWrapper, nested, nullErrorMessage)
				);
				recursor.runFlat("popContext", context ->
						context.output.popContext("element", rememberCounter)
				);
				counter += 1;
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	void readElements(Object value, int size, Recursor<ReadContext, ReadInfo> recursor) {
		for (int index = 0; index < size; index++) {
			final int rememberIndex = index;
			JobOutput<Boolean> hasValue = recursor.computeFlat("optional", context -> {
				if (valuesWrapper.field.optional && !context.input.read()) {
					if (value instanceof Collection<?>) {
						((Collection<Object>) value).add(null);
					} else {
						Array.set(value, rememberIndex, null);
					}
					return false;
				} else return true;
			});

			List<Object> rememberElement = new ArrayList<>(1);
			recursor.runNested("elements", read -> {
				if (!hasValue.get()) return;
				valuesWrapper.readValue(read, element -> {
					rememberElement.add(element);
					if (value instanceof Collection<?>) {
						((Collection<Object>) value).add(element);
					} else {
						Array.set(value, rememberIndex, element);
					}
				});
			});

			recursor.runFlat("referenceTargetLabel", read -> {
				if (hasValue.get() && valuesWrapper.field.referenceTargetLabel != null) {
					read.idLoader.register(
							valuesWrapper.field.referenceTargetLabel, rememberElement.get(0),
							read.input, recursor.info.bitser.cache
					);
				}
			});
		}
	}

	@Override
	void setLegacyValue(Recursor<ReadContext, ReadInfo> recursor, Object rawLegacyInstance, Consumer<Object> setValue) {
		if (rawLegacyInstance == null) {
			super.setLegacyValue(recursor, null, setValue);
			return;
		}

		LegacyCollectionInstance legacyInstance = (LegacyCollectionInstance) rawLegacyInstance;
		Object dummyArray = legacyInstance.newCollection.getClass().isArray() ? null : Array.newInstance(valuesWrapper.field.type, 1);
		int size = Array.getLength(legacyInstance.legacyArray);
		for (int index = 0; index < size; index++) {
			final int rememberIndex = index;
			final Object oldValue = Array.get(legacyInstance.legacyArray, index);

			recursor.runNested("elements", nested -> {
				try {
					if (legacyInstance.newCollection.getClass().isArray()) {
						valuesWrapper.setLegacyValue(nested, oldValue, newValue ->
								Array.set(legacyInstance.newCollection, rememberIndex, newValue)
						);
					} else {
						valuesWrapper.setLegacyValue(nested, oldValue, newValue -> {
							Array.set(dummyArray, 0, newValue);
							//noinspection unchecked
							((Collection<Object>) legacyInstance.newCollection).add(newValue);
						});
					}
				} catch (IllegalArgumentException wrongType) {
					throw new InvalidBitFieldException("Can't convert from legacy " + oldValue + " to " + valuesWrapper.field.type + " for field " + field);
				}
			});
		}

		super.setLegacyValue(recursor, legacyInstance.newCollection, setValue);
	}

	@Override
	void fixLegacyTypes(Recursor<ReadContext, ReadInfo> recursor, Object value) {
		if (value != null && !(value instanceof LegacyCollectionInstance)) {
			throw new InvalidBitFieldException("Can't convert from legacy " + value + " to " + valuesWrapper.field.type + " for field " + field);
		}
		super.fixLegacyTypes(recursor, value);
		if (value == null) return;
		LegacyCollectionInstance legacyInstance = (LegacyCollectionInstance) value;
		int size = Array.getLength(legacyInstance.legacyArray);
		for (int index = 0; index < size; index++) {
			final int rememberIndex = index;
			recursor.runNested("elements", nested ->
					valuesWrapper.fixLegacyTypes(nested, Array.get(legacyInstance.legacyArray, rememberIndex))
			);
		}
	}

	@Override
	boolean deepEquals(Object a, Object b, BitserCache cache) {
		if (a == null && b == null) return true;
		if (a == null || b == null) return false;
		if (field.type.isArray()) {
			int length = Array.getLength(a);
			if (Array.getLength(b) != length) return false;
			for (int index = 0; index < length; index++) {
				if (!valuesWrapper.deepEquals(Array.get(a, index), Array.get(b, index), cache)) return false;
			}
		} else {
			Iterator<?> iteratorA = ((Collection<?>) a).iterator();
			Iterator<?> iteratorB = ((Collection<?>) b).iterator();
			while (true) {
				if (iteratorA.hasNext() != iteratorB.hasNext()) return false;
				if (!iteratorA.hasNext()) break;
				if (!valuesWrapper.deepEquals(iteratorA.next(), iteratorB.next(), cache)) return false;
			}
		}

		return true;
	}

	@Override
	int hashCode(Object value, BitserCache cache) {
		if (value == null) return -10;

		int code = 7;
		if (field.type.isArray()) {
			int length = Array.getLength(value);
			for (int index = 0; index < length; index++) {
				code = 29 * code + valuesWrapper.hashCode(Array.get(value, index), cache);
			}
		} else {
			for (Object element : (Collection<?>) value) {
				code = 23 * code + valuesWrapper.hashCode(element, cache);
			}
		}

		return code;
	}
}
