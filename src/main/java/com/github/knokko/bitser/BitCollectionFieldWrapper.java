package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.exceptions.LegacyBitserException;
import com.github.knokko.bitser.legacy.LegacyCollectionInstance;
import com.github.knokko.bitser.field.ClassField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.util.JobOutput;
import com.github.knokko.bitser.util.Recursor;

import java.lang.reflect.Array;
import java.util.*;
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
				valuesWrapper.registerLegacyClasses(element, recursor);
			}
		} else {
			for (Object element : (Collection<?>) value) {
				valuesWrapper.registerLegacyClasses(element, recursor);
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
		valuesWrapper.collectReferenceLabels(recursor);
	}

	@Override
	void registerReferenceTargets(Object value, Recursor<ReferenceIdMapper, BitserCache> recursor) {
		super.registerReferenceTargets(value, recursor);
		if (value == null) return;
		if (field.type.isArray()) {
			int size = Array.getLength(value);
			for (int index = 0; index < size; index++) {
				Object element = Array.get(value, index);
				valuesWrapper.registerReferenceTargets(element, recursor);
			}
		} else {
			for (Object element : (Collection<?>) value) {
				valuesWrapper.registerReferenceTargets(element, recursor);
			}
		}
	}

	@Override
	void writeElements(Object value, int size, Recursor<WriteContext, WriteInfo> recursor) {
		String nullErrorMessage = "Field " + field + " must not have null elements";
		if (field.type.isArray()) {
			if (valuesWrapper.field.optional) {
				recursor.runFlat("optional", context -> {
					context.output.prepareProperty("optional", -1);
					for (int index = 0; index < size; index++) {
						context.output.write(Array.get(value, index) != null);
					}
					context.output.finishProperty();
				});
			}
			for (int index = 0; index < size; index++) {
				Object element = Array.get(value, index);
				if (element == null) {
					if (valuesWrapper.field.optional) continue;
					throw new InvalidBitValueException(nullErrorMessage);
				}

				if (recursor.info.usesContextInfo) {
					final int rememberIndex = index;
					recursor.runFlat("pushContext", context ->
							context.output.pushContext("element", rememberIndex)
					);
					recursor.runNested("element " + index, nested ->
							valuesWrapper.writeValue(element, nested)
					);
					recursor.runFlat("popContext", context ->
							context.output.popContext("element", rememberIndex)
					);
				} else {
					valuesWrapper.writeValue(element, recursor);
				}
			}
			if (valuesWrapper.field.referenceTargetLabel != null) {
				recursor.runFlat("referenceTargetLabel", context -> {
					context.output.pushContext("reference-target-label", -1);
					for (int index = 0; index < size; index++) {
						Object element = Array.get(value, index);
						if (element == null) continue;
						context.idMapper.maybeEncodeUnstableId(
								valuesWrapper.field.referenceTargetLabel, element, context.output, index
						);
					}
					context.output.popContext("reference-target-label", -1);
				});
			}
		} else {
			if (valuesWrapper.field.optional) {
				recursor.runFlat("optional", context -> {
					context.output.prepareProperty("optional", -1);
					for (Object element : (Collection<?>) value) {
						context.output.write(element != null);
					}
					context.output.finishProperty();
				});
			}

			int index = -1;
			for (Object element : (Collection<?>) value) {
				index += 1;
				if (element == null) {
					if (valuesWrapper.field.optional) continue;
					throw new InvalidBitValueException(nullErrorMessage);
				}

				if (recursor.info.usesContextInfo) {
					final int rememberIndex = index;
					recursor.runFlat("pushContext", context ->
							context.output.pushContext("element", rememberIndex)
					);
					recursor.runNested("element " + index, nested ->
							valuesWrapper.writeValue(element, nested)
					);
					recursor.runFlat("popContext", context ->
							context.output.popContext("element", rememberIndex)
					);
				} else {
					valuesWrapper.writeValue(element, recursor);
				}
			}

			if (valuesWrapper.field.referenceTargetLabel != null) {
				recursor.runFlat("referenceTargetLabel", context -> {
					context.output.pushContext("reference-target-label", -1);
					int counter = -1;
					for (Object element : (Collection<?>) value) {
						counter += 1;
						if (element == null) continue;
						context.idMapper.maybeEncodeUnstableId(
								valuesWrapper.field.referenceTargetLabel, element, context.output, counter
						);
					}
					context.output.popContext("reference-target-label", -1);
				});
			}
		}
	}

	@Override
	void registerReferenceTargets(
			ReferenceTracker references, Object value,
			RecursionNode parentNode, String fieldName
	) {
		RecursionNode childNode = new RecursionNode(parentNode, fieldName);
		if (field.type.isArray()) {
			references.arrayJobs.add(new WithArrayJob(value, valuesWrapper, childNode));
		} else {
			references.arrayJobs.add(new WithArrayJob(((Collection<?>) value).toArray(), valuesWrapper, childNode));
		}
	}

	@Override
	public void write(Serializer serializer, Object value, RecursionNode parentNode, String fieldName) throws Throwable {
		RecursionNode childNode = new RecursionNode(parentNode, fieldName);
		if (field.type.isArray()) {
			int length = Array.getLength(value);
			if (serializer.intDistribution != null) {
				serializer.intDistribution.insert(field + " collection size", (long) length, sizeField);
				serializer.intDistribution.insert("collection size", (long) length, sizeField);
			}

			serializer.output.prepareProperty("array-length", -1);
			IntegerBitser.encodeInteger(length, sizeField, serializer.output);
			serializer.output.finishProperty();

			if (length == 0) return;
			if (valuesWrapper instanceof ReferenceFieldWrapper) {
				serializer.arrayReferenceJobs.add(new WriteArrayReferenceJob(
						value, (ReferenceFieldWrapper) valuesWrapper, childNode
				));
			} else {
				serializer.arrayJobs.add(new WriteArrayJob(
						value, valuesWrapper, childNode, "this array must not have null elements"
				));
			}
		} else {
			Collection<?> collection = (Collection<?>) value;
			if (serializer.intDistribution != null) {
				serializer.intDistribution.insert(field + " collection size", (long) collection.size(), sizeField);
				serializer.intDistribution.insert("collection size", (long) collection.size(), sizeField);
			}

			serializer.output.prepareProperty("collection-size", -1);
			IntegerBitser.encodeInteger(collection.size(), sizeField, serializer.output);
			serializer.output.finishProperty();

			if (collection.isEmpty()) return;
			if (valuesWrapper instanceof ReferenceFieldWrapper) {
				serializer.arrayReferenceJobs.add(new WriteArrayReferenceJob(
						collection.toArray(), (ReferenceFieldWrapper) valuesWrapper, childNode
				));
			} else {
				serializer.arrayJobs.add(new WriteArrayJob(
						collection.toArray(), valuesWrapper, childNode,
						"this collection must not have null elements"
				));
			}
		}
	}

	@Override
	public Object read(Deserializer deserializer, RecursionNode parentNode, String fieldName) throws Throwable {
		RecursionNode childNode = new RecursionNode(parentNode, fieldName);
		if (field.type.isArray()) {
			deserializer.input.prepareProperty("array-length", -1);
			int length = IntegerBitser.decodeLength(sizeField, deserializer.sizeLimit, "array-length", deserializer.input);
			deserializer.input.finishProperty();

			Object array = Array.newInstance(field.type.getComponentType(), length);
			if (length == 0) return array;

			if (valuesWrapper instanceof ReferenceFieldWrapper) {
				deserializer.arrayReferenceJobs.add(new ReadArrayReferenceJob(
						array, (ReferenceFieldWrapper) valuesWrapper, childNode
				));
			} else {
				deserializer.arrayJobs.add(new ReadArrayJob(
						array, valuesWrapper, childNode
				));
			}

			return array;
		} else {
			deserializer.input.prepareProperty("collection-size", -1);
			int size = IntegerBitser.decodeLength(sizeField, deserializer.sizeLimit, "collection-size", deserializer.input);
			deserializer.input.finishProperty();

			Object[] array = new Object[size];
			Collection<?> collection = (Collection<?>) constructCollectionWithSize(field.type, size);
			if (size == 0) return collection;

			if (valuesWrapper instanceof ReferenceFieldWrapper) {
				deserializer.arrayReferenceJobs.add(new ReadArrayReferenceJob(
						array, (ReferenceFieldWrapper) valuesWrapper, childNode
				));
			} else {
				deserializer.arrayJobs.add(new ReadArrayJob(
						array, valuesWrapper, childNode
				));
			}
			deserializer.populateJobs.add(new PopulateCollectionJob(collection, array, childNode));

			return collection;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	void readElements(Object value, int size, Recursor<ReadContext, ReadInfo> recursor) {
		JobOutput<boolean[]> hasValues = recursor.computeFlat("optional", context -> {
			if (valuesWrapper.field.optional) {
				boolean[] hadValues = new boolean[size];
				for (int index = 0; index < size; index++) {
					hadValues[index] = context.input.read();
				}
				return hadValues;
			} else {
				return null;
			}
		});

		Object[] rememberElements = new Object[size];
		recursor.runNested("elements", nested -> {
			boolean[] hadValues = hasValues.get();

			for (int index = 0; index < size; index++) {
				if (hadValues != null && !hadValues[index]) {
					if (value instanceof Collection<?>) {
						nested.runFlat("add-null-element", context ->
								((Collection<Object>) value).add(null)
						);
					} else {
						Array.set(value, index, null);
					}
					continue;
				}

				final int rememberIndex = index;
				valuesWrapper.readValue(nested, element -> {
					rememberElements[rememberIndex] = element;
					if (value instanceof Collection<?>) {
						((Collection<Object>) value).add(element);
					} else {
						Array.set(value, rememberIndex, element);
					}
				});
			}
		});

		if (valuesWrapper.field.referenceTargetLabel != null) {
			recursor.runFlat("referenceTargetLabel", read -> {
				boolean[] hadValues = hasValues.get();

				for (int index = 0; index < size; index++) {
					if (hadValues != null && !hadValues[index]) continue;
					read.idLoader.register(
							valuesWrapper.field.referenceTargetLabel, rememberElements[index],
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
		Object dummyArray = legacyInstance.newCollection.getClass().isArray() ? null :
				Array.newInstance(valuesWrapper.field.type, 1);
		int size = Array.getLength(legacyInstance.legacyArray);

		if (size > 0) {
			recursor.runNested("elements", nested -> {
				for (int index = 0; index < size; index++) {
					Object oldValue = Array.get(legacyInstance.legacyArray, index);
					final int rememberIndex = index;
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
						throw new LegacyBitserException("Can't convert from legacy " + oldValue + " to " + valuesWrapper.field.type + " for field " + field);
					}
				}
			});
		}

		super.setLegacyValue(recursor, legacyInstance.newCollection, setValue);
	}

	@Override
	void fixLegacyTypes(Recursor<ReadContext, ReadInfo> recursor, Object value) {
		if (value != null && !(value instanceof LegacyCollectionInstance)) {
			throw new LegacyBitserException("Can't convert from legacy " + value + " to " + valuesWrapper.field.type + " for field " + field);
		}
		super.fixLegacyTypes(recursor, value);
		if (value == null) return;
		LegacyCollectionInstance legacyInstance = (LegacyCollectionInstance) value;
		int size = Array.getLength(legacyInstance.legacyArray);

		if (size > 0) {
			recursor.runNested("elements", nested -> {
				for (int index = 0; index < size; index++) {
					valuesWrapper.fixLegacyTypes(nested, Array.get(legacyInstance.legacyArray, index));
				}
			});
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
