package com.lowes.permits.exception;

public class PermitNotFoundException extends RuntimeException {
	public PermitNotFoundException(String message) {
		super(message);
	}

	public PermitNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}
}
