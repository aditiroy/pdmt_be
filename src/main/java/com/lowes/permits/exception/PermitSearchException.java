package com.lowes.permits.exception;

public class PermitSearchException extends RuntimeException {
	public PermitSearchException(String message) {
		super(message);
	}

	public PermitSearchException(String message, Throwable cause) {
		super(message, cause);
	}
}
