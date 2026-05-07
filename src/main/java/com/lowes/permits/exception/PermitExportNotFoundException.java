package com.lowes.permits.exception;

public class PermitExportNotFoundException extends RuntimeException {
	public PermitExportNotFoundException(String message) {
		super(message);
	}

	public PermitExportNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}
}
