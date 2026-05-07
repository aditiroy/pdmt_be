package com.lowes.permits.exception;

public class InvalidUserTokenException extends RuntimeException {
	public InvalidUserTokenException(String message) {
		super(message);
	}

	public InvalidUserTokenException(String message, Throwable cause) {
		super(message, cause);
	}
}
