package com.github.knokko.bitser;

import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.field.StringField;
import com.github.knokko.bitser.util.Recursor;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import static java.lang.Math.max;
import static java.lang.Math.min;

@BitStruct(backwardCompatible = false)
class StringFieldWrapper extends BitFieldWrapper {

	@BitField
	private final IntegerField.Properties lengthField;

	StringFieldWrapper(VirtualField field, StringField stringField) {
		super(field);
		long minLength = 0;
		long maxLength = Integer.MAX_VALUE;
		boolean expectUniform = false;
		int digitSize = IntegerField.DIGIT_SIZE_TERMINATORS;
		long[] commonValues = new long[0];
		if (stringField != null) {
			minLength = max(minLength, stringField.length().minValue());
			maxLength = min(maxLength, stringField.length().maxValue());
			expectUniform = stringField.length().expectUniform();
			digitSize = stringField.length().digitSize();
			commonValues = stringField.length().commonValues();
		}
		this.lengthField = new IntegerField.Properties(minLength, maxLength, expectUniform, digitSize, commonValues);
	}

	@SuppressWarnings("unused")
	private StringFieldWrapper() {
		super();
		this.lengthField = new IntegerField.Properties();
	}

	@Override
	void writeValue(Object value, Recursor<WriteContext, WriteInfo> recursor) {
		recursor.runFlat("string", context -> {
			if (context.integerDistribution != null) {
				long length = ((String) value).getBytes(StandardCharsets.UTF_8).length;
				context.integerDistribution.insert(field + " string length", length, lengthField);
				context.integerDistribution.insert("string length", length, lengthField);
			}
			StringBitser.encode((String) value, lengthField, context.output);
		});
	}

	@Override
	void readValue(Recursor<ReadContext, ReadInfo> recursor, Consumer<Object> setValue) {
		recursor.runFlat("string-value", context ->
				setValue.accept(StringBitser.decode(lengthField, recursor.info.sizeLimit, context.input))
		);
	}
}
