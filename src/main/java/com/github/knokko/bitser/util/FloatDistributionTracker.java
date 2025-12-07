package com.github.knokko.bitser.util;

import com.github.knokko.bitser.FloatBitser;
import com.github.knokko.bitser.field.FloatField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.io.BitCountStream;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static java.lang.Math.min;

public class FloatDistributionTracker extends DistributionTracker<Double> {

	public static class Entry {
		int digitSize;
		double expectMultipleOf;
		double[] commonValues;
		int spentBits;

		@Override
		public String toString() {
			return "(spentBits=" + spentBits + ", digitSize=" + (digitSize == -1 ? "uniform" : digitSize) +
					", expectMultipleOf=" + expectMultipleOf + ", commonValues=" + Arrays.toString(commonValues) + ")";
		}
	}

	@Override
	public List<String> getSortedFields() {
		List<String> fields = new ArrayList<>(mapping.keySet());
		fields.sort((a, b) -> -Long.compare(countBits(a), countBits(b)));
		return fields;
	}

	private int countBits(String field) {
		Field<Double> fieldData = mapping.get(field);
		return countBits(fieldData, (FloatField.Properties) fieldData.properties);
	}

	private int countBits(Field<Double> fieldData, FloatField.Properties properties) {
		boolean doublePrecision = fieldData.type == double.class || fieldData.type == Double.class;
		BitCountStream bitCounter = new BitCountStream();
		fieldData.countMap.forEach((value, occurrences) -> {
			for (int counter = 0; counter < occurrences; counter++) {
				try {
					FloatBitser.encodeFloat(value, doublePrecision, properties, bitCounter);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		});
		return bitCounter.getCounter();
	}

	@SuppressWarnings("OptionalGetWithoutIsPresent")
	public void optimize(
			PrintWriter writer, int maxFields, int maxCommonValues,
			int maxSuggestions, double[] expectMultipleOfCandidates
	) {
		int remainingFields = maxFields;
		for (String field : getSortedFields()) {
			if (remainingFields <= 0) break;

			writer.println("Field " + field + ": (x " + count(field) + ")");
			Field<Double> fieldData = mapping.get(field);
			FloatField.Properties properties = (FloatField.Properties) fieldData.properties;

			List<Entry> entries = optimize(field, maxCommonValues, expectMultipleOfCandidates);
			if (entries.get(0).spentBits >= countBits(fieldData, properties)) {
				writer.println("  already optimal");
				writer.println();
				continue;
			} writer.println("  suboptimal: currently uses " + countBits(field) + " bits");

			writer.println("  used interval is [" + fieldData.countMap.keySet().stream().min(Double::compare).get() + ", " +
					fieldData.countMap.keySet().stream().max(Double::compare).get() + "]");

			int remainingSuggestions = maxSuggestions;
			for (Entry entry : entries) {
				if (remainingSuggestions-- <= 0) break;
				writer.println("  " + entry);
			}
			writer.println();
			remainingFields -= 1;
		}
		writer.flush();
	}

	public List<Entry> optimize(String field, int maxCommonValues, double[] expectMultipleOfCandidates) {
		List<Entry> entries = new ArrayList<>((1 + maxCommonValues) * 7);

		Field<Double> fieldData = mapping.get(field);
		FloatField.Properties originalProperties = (FloatField.Properties) fieldData.properties;

		List<Double> mostCommonValues = new ArrayList<>(fieldData.countMap.keySet());
		mostCommonValues.sort((a, b) -> -Integer.compare(fieldData.countMap.get(a), fieldData.countMap.get(b)));

		for (double expectMultipleOf : expectMultipleOfCandidates) {
			for (int numCommonValues = 0; numCommonValues <= maxCommonValues; numCommonValues++) {
				double[] commonValues = new double[min(numCommonValues, mostCommonValues.size())];
				for (int index = 0; index < commonValues.length; index++) {
					commonValues[index] = mostCommonValues.get(index);
				}

				for (int digitSize = 0; digitSize <= 7; digitSize++) {
					if (digitSize == 1) continue;

					FloatField.Properties properties = new FloatField.Properties(
							expectMultipleOf, originalProperties.errorTolerance,
							new IntegerField.Properties(
									originalProperties.expectedIntegerMultiple.minValue,
									originalProperties.expectedIntegerMultiple.maxValue,
									false, digitSize, new long[0]
							),
							commonValues
					);
					Entry entry = new Entry();
					entry.digitSize = digitSize;
					entry.expectMultipleOf = expectMultipleOf;
					entry.commonValues = commonValues;
					entry.spentBits = countBits(fieldData, properties);
					entries.add(entry);
				}
			}
		}

		entries.sort(Comparator.comparingInt(a -> a.spentBits));
		return entries;
	}
}
