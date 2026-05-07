package com.lowes.permits.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PermitPostgresEntitySearchExceptionTest {

	@Test
	void testConstructorWithMessage() {
		String message = "Permit search failed due to invalid query parameters";
		PermitSearchException exception = new PermitSearchException(message);

		assertEquals(message, exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	void testConstructorWithMessageAndCause() {
		String message = "Permit search failed due to database error";
		Throwable cause = new RuntimeException("Connection timeout");
		PermitSearchException exception = new PermitSearchException(message, cause);

		assertEquals(message, exception.getMessage());
		assertEquals(cause, exception.getCause());
	}

	@Test
	void testInheritanceFromRuntimeException() {
		PermitSearchException exception = new PermitSearchException("test message");

		assertInstanceOf(RuntimeException.class, exception);
		assertInstanceOf(RuntimeException.class, exception);
	}

	@Test
	void testWithNullMessage() {
		PermitSearchException exception = new PermitSearchException(null);

		assertNull(exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	void testWithEmptyMessage() {
		String emptyMessage = "";
		PermitSearchException exception = new PermitSearchException(emptyMessage);

		assertEquals(emptyMessage, exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	void testWithNullCause() {
		String message = "Test message";
		PermitSearchException exception = new PermitSearchException(message, null);

		assertEquals(message, exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	void testWithComplexMessage() {
		String complexMessage =
				"Permit search failed: Invalid query syntax. The search query contains unsupported operators or malformed filters. Please check your query parameters and refer to the API documentation for proper syntax.";
		PermitSearchException exception = new PermitSearchException(complexMessage);

		assertEquals(complexMessage, exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	void testWithNestedCause() {
		String message = "Search service unavailable";
		IllegalStateException nestedCause = new IllegalStateException("Search service is down for maintenance");
		PermitSearchException exception = new PermitSearchException(message, nestedCause);

		assertEquals(message, exception.getMessage());
		assertEquals(nestedCause, exception.getCause());
		assertInstanceOf(IllegalStateException.class, nestedCause);
	}

	@Test
	void testExceptionChaining() {
		String message = "Permit search error";
		RuntimeException rootCause = new RuntimeException("Elasticsearch connection failed");
		PermitSearchException exception = new PermitSearchException(message, rootCause);

		// Test exception chaining
		assertEquals(rootCause, exception.getCause());
		assertEquals("Elasticsearch connection failed", exception.getCause().getMessage());
	}

	@Test
	void testStackTrace() {
		String message = "Test exception";
		PermitSearchException exception = new PermitSearchException(message);

		// Verify that stack trace is available
		assertNotNull(exception.getStackTrace());
		assertTrue(exception.getStackTrace().length > 0);
	}

	@Test
	void testToString() {
		String message = "Test message";
		PermitSearchException exception = new PermitSearchException(message);

		String toString = exception.toString();

		assertTrue(toString.contains("PermitSearchException"));
		assertTrue(toString.contains(message));
	}

	@Test
	void testWithSpecialCharactersInMessage() {
		String specialMessage = "Search failed with special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?";
		PermitSearchException exception = new PermitSearchException(specialMessage);

		assertEquals(specialMessage, exception.getMessage());
	}

	@Test
	void testWithUnicodeInMessage() {
		String unicodeMessage = "Búsqueda fallida: ñáéíóú 中文 العربية";
		PermitSearchException exception = new PermitSearchException(unicodeMessage);

		assertEquals(unicodeMessage, exception.getMessage());
	}

	@Test
	void testWithLongMessage() {
		StringBuilder longMessageBuilder = new StringBuilder();
		for (int i = 0; i < 1000; i++) {
			longMessageBuilder.append("Search error iteration ").append(i).append(". ");
		}
		String longMessage = longMessageBuilder.toString();

		PermitSearchException exception = new PermitSearchException(longMessage);

		assertEquals(longMessage, exception.getMessage());
		assertTrue(longMessage.length() > 1000);
	}

	@Test
	void testExceptionCanBeCaughtAsRuntimeException() {
		// Test that the exception can be caught as RuntimeException
		try {
			throw new PermitSearchException("Test message");
		} catch (RuntimeException e) {
			assertInstanceOf(PermitSearchException.class, e);
			assertEquals("Test message", e.getMessage());
		}
	}

	@Test
	void testExceptionCanBeCaughtAsPermitSearchException() {
		// Test that the exception can be caught as its specific type
		try {
			throw new PermitSearchException("Test message");
		} catch (PermitSearchException e) {
			assertEquals("Test message", e.getMessage());
		}
	}

	@Test
	void testWithNetworkExceptionAsCause() {
		String message = "Search service communication failed";
		Exception networkException = new java.net.ConnectException("Connection refused");
		PermitSearchException exception = new PermitSearchException(message, networkException);

		assertEquals(message, exception.getMessage());
		assertEquals(networkException, exception.getCause());
		assertEquals("Connection refused", exception.getCause().getMessage());
	}

	@Test
	void testWithTimeoutExceptionAsCause() {
		String message = "Search operation timed out";
		Exception timeoutException = new java.util.concurrent.TimeoutException("Search timeout after 30 seconds");
		PermitSearchException exception = new PermitSearchException(message, timeoutException);

		assertEquals(message, exception.getMessage());
		assertEquals(timeoutException, exception.getCause());
		assertEquals("Search timeout after 30 seconds", exception.getCause().getMessage());
	}

	@Test
	void testWithMultipleCauses() {
		String message = "Search failed";
		RuntimeException rootCause = new RuntimeException("Root cause");
		RuntimeException intermediateCause = new RuntimeException("Intermediate cause", rootCause);
		PermitSearchException exception = new PermitSearchException(message, intermediateCause);

		assertEquals(message, exception.getMessage());
		assertEquals(intermediateCause, exception.getCause());
		assertEquals(rootCause, exception.getCause().getCause());
	}

	@Test
	void testWithJsonProcessingExceptionAsCause() {
		String message = "Failed to parse search response";
		Exception jsonException = new com.fasterxml.jackson.core.JsonProcessingException("Invalid JSON format") {
			@Override
			public String getMessage() {
				return "Invalid JSON format";
			}
		};
		PermitSearchException exception = new PermitSearchException(message, jsonException);

		assertEquals(message, exception.getMessage());
		assertEquals(jsonException, exception.getCause());
		assertEquals("Invalid JSON format", exception.getCause().getMessage());
	}
}
