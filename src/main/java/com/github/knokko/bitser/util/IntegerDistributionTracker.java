package com.github.knokko.bitser.util;

import com.github.knokko.bitser.IntegerBitser;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.io.BitCountStream;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import static java.lang.Math.min;

public class IntegerDistributionTracker extends DistributionTracker<Long> {

	public static class Entry {
		public int digitSize;
		public long[] commonValues;
		public int spentBits;

		@Override
		public String toString() {
			return "(spentBits=" + spentBits + ", digitSize=" + (digitSize == -1 ? "uniform" : digitSize) +
					", commonValues=" + Arrays.toString(commonValues) + ")";
		}
	}

	@Override
	public List<String> getSortedFields() {
		List<String> fields = new ArrayList<>(mapping.keySet());
		fields.sort((a, b) -> -Long.compare(countBits(a), countBits(b)));
		return fields;
	}

	private int countBits(String field) {
		Field<Long> fieldData = mapping.get(field);
		return countBits(fieldData, (IntegerField.Properties) fieldData.properties);
	}

	@SuppressWarnings("OptionalGetWithoutIsPresent")
	public void optimize(PrintWriter writer, int maxFields, int maxCommonValues, int maxSuggestions) {
		int remainingFields = maxFields;
		for (String field : getSortedFields()) {
			if (remainingFields <= 0) break;

			writer.println("Field " + field + ": (x " + count(field) + ")");
			Field<Long> fieldData = mapping.get(field);
			IntegerField.Properties properties = (IntegerField.Properties) fieldData.properties;

			List<Entry> entries = optimize(field, maxCommonValues);
			if (entries.get(0).spentBits >= countBits(fieldData, properties)) {
				writer.println("  already optimal");
				writer.println();
				continue;
			} writer.println("  suboptimal: currently uses " + countBits(field) + " bits");

			writer.println("  allowed interval is [" + properties.minValue + ", " + properties.maxValue + "]");
			writer.println("  used interval is [" + fieldData.countMap.keySet().stream().min(Long::compare).get() + ", " +
					fieldData.countMap.keySet().stream().max(Long::compare).get() + "]");

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

	public List<Entry> optimize(String field, int maxCommonValues) {
		List<Entry> entries = new ArrayList<>((1 + maxCommonValues) * 7);

		Field<Long> fieldData = mapping.get(field);
		IntegerField.Properties originalProperties = (IntegerField.Properties) fieldData.properties;

		List<Long> mostCommonValues = new ArrayList<>(fieldData.countMap.keySet());
		mostCommonValues.sort((a, b) -> -Integer.compare(fieldData.countMap.get(a), fieldData.countMap.get(b)));

		for (int numCommonValues = 0; numCommonValues <= maxCommonValues; numCommonValues++) {
			long[] commonValues = new long[min(numCommonValues, mostCommonValues.size())];
			for (int index = 0; index < commonValues.length; index++) {
				commonValues[index] = mostCommonValues.get(index);
			}

			for (int digitSize = 0; digitSize <= 7; digitSize++) {
				if (digitSize == 1) continue;

				IntegerField.Properties properties = new IntegerField.Properties(
						originalProperties.minValue, originalProperties.maxValue,
						false, digitSize, commonValues
				);
				Entry entry = new Entry();
				entry.digitSize = digitSize;
				entry.commonValues = commonValues;
				entry.spentBits = countBits(fieldData, properties);
				entries.add(entry);
			}

			IntegerField.Properties properties = new IntegerField.Properties(
					originalProperties.minValue, originalProperties.maxValue,
					true, 0, commonValues
			);
			Entry entry = new Entry();
			entry.digitSize = -1;
			entry.commonValues = commonValues;
			entry.spentBits = countBits(fieldData, properties);
			entries.add(entry);
		}

		entries.sort(Comparator.comparingInt(a -> a.spentBits));
		return entries;
	}

	private int countBits(Field<Long> fieldData, IntegerField.Properties properties) {
		BitCountStream bitCounter = new BitCountStream();
		fieldData.countMap.forEach((value, occurrences) -> {
			for (int counter = 0; counter < occurrences; counter++) {
				try {
					IntegerBitser.encodeInteger(value, properties, bitCounter);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		});
		return bitCounter.getCounter();
	}
}
