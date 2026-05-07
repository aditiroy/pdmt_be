package com.lowes.permits.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PermitExportNotFoundExceptionTest {

	@Test
	void testConstructorWithMessage() {
		String message = "No permit report data found. Please generate the report first.";
		PermitExportNotFoundException exception = new PermitExportNotFoundException(message);

		assertEquals(message, exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	void testConstructorWithMessageAndCause() {
		String message = "Permit file not found in storage. The file may have been deleted or moved.";
		Throwable cause = new RuntimeException("S3 connection failed");
		PermitExportNotFoundException exception = new PermitExportNotFoundException(message, cause);

		assertEquals(message, exception.getMessage());
		assertEquals(cause, exception.getCause());
	}

	@Test
	void testInheritanceFromRuntimeException() {
		PermitExportNotFoundException exception = new PermitExportNotFoundException("test message");

		assertInstanceOf(RuntimeException.class, exception);
	}

	@Test
	void testWithNullMessage() {
		PermitExportNotFoundException exception = new PermitExportNotFoundException(null);

		assertNull(exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	void testWithEmptyMessage() {
		String emptyMessage = "";
		PermitExportNotFoundException exception = new PermitExportNotFoundException(emptyMessage);

		assertEquals(emptyMessage, exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	void testWithNullCause() {
		String message = "Test message";
		PermitExportNotFoundException exception = new PermitExportNotFoundException(message, null);

		assertEquals(message, exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	void testWithS3ExceptionAsCause() {
		String message = "Permit file not found in storage";
		Exception s3Exception = new RuntimeException("NoSuchKey: The specified key does not exist");
		PermitExportNotFoundException exception = new PermitExportNotFoundException(message, s3Exception);

		assertEquals(message, exception.getMessage());
		assertEquals(s3Exception, exception.getCause());
		assertTrue(exception.getCause().getMessage().contains("NoSuchKey"));
	}

	@Test
	void testExceptionChaining() {
		String message = "Export file not found";
		RuntimeException rootCause = new RuntimeException("S3 bucket not accessible");
		PermitExportNotFoundException exception = new PermitExportNotFoundException(message, rootCause);

		assertEquals(rootCause, exception.getCause());
		assertEquals("S3 bucket not accessible", exception.getCause().getMessage());
	}

	@Test
	void testStackTrace() {
		String message = "Test exception";
		PermitExportNotFoundException exception = new PermitExportNotFoundException(message);

		assertNotNull(exception.getStackTrace());
		assertTrue(exception.getStackTrace().length > 0);
	}

	@Test
	void testToString() {
		String message = "No permit report data found";
		PermitExportNotFoundException exception = new PermitExportNotFoundException(message);

		String toString = exception.toString();

		assertTrue(toString.contains("PermitExportNotFoundException"));
		assertTrue(toString.contains(message));
	}

	@Test
	void testExceptionCanBeCaughtAsRuntimeException() {
		try {
			throw new PermitExportNotFoundException("Test message");
		} catch (RuntimeException e) {
			assertInstanceOf(PermitExportNotFoundException.class, e);
			assertEquals("Test message", e.getMessage());
		}
	}

	@Test
	void testExceptionCanBeCaughtAsPermitExportNotFoundException() {
		try {
			throw new PermitExportNotFoundException("Test message");
		} catch (PermitExportNotFoundException e) {
			assertEquals("Test message", e.getMessage());
		}
	}

	@Test
	void testWithMongoExceptionAsCause() {
		String message = "Export metadata not found";
		Exception mongoException = new RuntimeException("MongoDB connection timeout");
		PermitExportNotFoundException exception = new PermitExportNotFoundException(message, mongoException);

		assertEquals(message, exception.getMessage());
		assertEquals(mongoException, exception.getCause());
		assertEquals("MongoDB connection timeout", exception.getCause().getMessage());
	}

	@Test
	void testWithMultipleCauses() {
		String message = "Export file not accessible";
		RuntimeException rootCause = new RuntimeException("Network error");
		RuntimeException intermediateCause = new RuntimeException("S3 request failed", rootCause);
		PermitExportNotFoundException exception = new PermitExportNotFoundException(message, intermediateCause);

		assertEquals(message, exception.getMessage());
		assertEquals(intermediateCause, exception.getCause());
		assertEquals(rootCause, exception.getCause().getCause());
	}

	@Test
	void testWithRealisticNoMetadataMessage() {
		String message = "No permit report data found. Please generate the report first.";
		PermitExportNotFoundException exception = new PermitExportNotFoundException(message);

		assertEquals(message, exception.getMessage());
		assertTrue(message.contains("report"));
		assertTrue(message.contains("generate"));
	}

	@Test
	void testWithRealisticS3NotFoundMessage() {
		String message = "Permit file not found in storage. The file may have been deleted or moved.";
		PermitExportNotFoundException exception = new PermitExportNotFoundException(message);

		assertEquals(message, exception.getMessage());
		assertTrue(message.contains("not found"));
		assertTrue(message.contains("storage"));
	}
}
