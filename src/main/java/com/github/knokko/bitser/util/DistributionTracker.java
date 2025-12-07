package com.github.knokko.bitser.util;

import java.io.PrintWriter;
import java.util.*;

public class DistributionTracker<T> {

	public final Map<String, Field<T>> mapping = new HashMap<>();

	public void insert(String description, T value, Object properties) {
		mapping.computeIfAbsent(description, key -> new Field<>(value.getClass(), properties)).insert(value);
	}

	public long count(String field) {
		return mapping.get(field).count();
	}

	public List<String> getSortedFields() {
		List<String> fields = new ArrayList<>(mapping.keySet());
		fields.sort((a, b) -> -Long.compare(count(a), count(b)));
		return fields;
	}

	public void printFieldOccurrences(PrintWriter writer) {
		for (String field : getSortedFields()) {
			writer.println(count(field) + " x " + field);
		}
		writer.flush();
	}

	public List<T> getSortedValues(Map<T, Integer> countMap) {
		List<T> values = new ArrayList<>(countMap.keySet());
		values.sort(Comparator.comparingInt(countMap::get).reversed());
		return values;
	}

	public void printFieldValueOccurrences(PrintWriter writer, int maxFields, int maxValuesPerField) {
		int remainingFields = maxFields;
		for (String field : getSortedFields()) {
			if (remainingFields-- <= 0) break;
			Field<T> fieldData = mapping.get(field);
			writer.println(count(field) + " x " + field + ":");

			int remainingValues = maxValuesPerField;
			for (T value : getSortedValues(fieldData.countMap)) {
				if (remainingValues-- <= 0) break;
				writer.println("  " + fieldData.countMap.get(value) + " x " + value);
			}
		}
		writer.flush();
	}

	public static class Field<T> {

		public final Class<?> type;
		public final Object properties;
		public final Map<T, Integer> countMap = new HashMap<>();

		public Field(Class<?> type, Object properties) {
			this.type = type;
			this.properties = properties;
		}

		public void insert(T value) {
			countMap.put(value, countMap.getOrDefault(value, 0) + 1);
		}

		public long count() {
			return countMap.values().stream().mapToLong(x -> x).sum();
		}
	}
}
