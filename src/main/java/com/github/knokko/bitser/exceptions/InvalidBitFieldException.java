package com.github.knokko.bitser.exceptions;

/**
 * This exception is thrown by bitser when it tries to serialize the value of an invalid field (e.g. because the type
 * of the field is an abstract class).
 */
public class InvalidBitFieldException extends BitserException {

	public InvalidBitFieldException(String message) {
		super(message);
	}
}
