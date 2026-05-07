package com.lowes.permits.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PermitPostgresEntityNotFoundExceptionTest {

	@Test
	void testConstructorWithMessage() {
		String message = "Permit not found for ID: 123";
		PermitNotFoundException exception = new PermitNotFoundException(message);

		assertEquals(message, exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	void testConstructorWithMessageAndCause() {
		String message = "Permit not found for ID: 456";
		Throwable cause = new RuntimeException("Database connection failed");
		PermitNotFoundException exception = new PermitNotFoundException(message, cause);

		assertEquals(message, exception.getMessage());
		assertEquals(cause, exception.getCause());
	}

	@Test
	void testInheritanceFromRuntimeException() {
		PermitNotFoundException exception = new PermitNotFoundException("test message");

		assertInstanceOf(RuntimeException.class, exception);
		assertInstanceOf(RuntimeException.class, exception);
	}

	@Test
	void testWithNullMessage() {
		PermitNotFoundException exception = new PermitNotFoundException(null);

		assertNull(exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	void testWithEmptyMessage() {
		String emptyMessage = "";
		PermitNotFoundException exception = new PermitNotFoundException(emptyMessage);

		assertEquals(emptyMessage, exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	void testWithNullCause() {
		String message = "Test message";
		PermitNotFoundException exception = new PermitNotFoundException(message, null);

		assertEquals(message, exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	void testWithComplexMessage() {
		String complexMessage =
				"Permit not found for permitDbId: 'PERMIT-123-ABC'. The permit may have been deleted or the ID might be incorrect. Please verify the permit ID and try again.";
		PermitNotFoundException exception = new PermitNotFoundException(complexMessage);

		assertEquals(complexMessage, exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	void testWithNestedCause() {
		String message = "Permit lookup failed";
		IllegalArgumentException nestedCause = new IllegalArgumentException("Invalid permit ID format");
		PermitNotFoundException exception = new PermitNotFoundException(message, nestedCause);

		assertEquals(message, exception.getMessage());
		assertEquals(nestedCause, exception.getCause());
		assertInstanceOf(IllegalArgumentException.class, nestedCause);
	}

	@Test
	void testExceptionChaining() {
		String message = "Permit not found";
		RuntimeException rootCause = new RuntimeException("Database error");
		PermitNotFoundException exception = new PermitNotFoundException(message, rootCause);

		// Test exception chaining
		assertEquals(rootCause, exception.getCause());
		assertEquals("Database error", exception.getCause().getMessage());
	}

	@Test
	void testStackTrace() {
		String message = "Test exception";
		PermitNotFoundException exception = new PermitNotFoundException(message);

		// Verify that stack trace is available
		assertNotNull(exception.getStackTrace());
		assertTrue(exception.getStackTrace().length > 0);
	}

	@Test
	void testToString() {
		String message = "Test message";
		PermitNotFoundException exception = new PermitNotFoundException(message);

		String toString = exception.toString();

		assertTrue(toString.contains("PermitNotFoundException"));
		assertTrue(toString.contains(message));
	}

	@Test
	void testWithSpecialCharactersInMessage() {
		String specialMessage = "Permit not found with special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?";
		PermitNotFoundException exception = new PermitNotFoundException(specialMessage);

		assertEquals(specialMessage, exception.getMessage());
	}

	@Test
	void testWithUnicodeInMessage() {
		String unicodeMessage = "Permiso no encontrado: ñáéíóú 中文 العربية";
		PermitNotFoundException exception = new PermitNotFoundException(unicodeMessage);

		assertEquals(unicodeMessage, exception.getMessage());
	}

	@Test
	void testWithLongMessage() {
		StringBuilder longMessageBuilder = new StringBuilder();
		for (int i = 0; i < 1000; i++) {
			longMessageBuilder.append("Permit not found - iteration ").append(i).append(". ");
		}
		String longMessage = longMessageBuilder.toString();

		PermitNotFoundException exception = new PermitNotFoundException(longMessage);

		assertEquals(longMessage, exception.getMessage());
		assertTrue(longMessage.length() > 1000);
	}

	@Test
	void testExceptionCanBeCaughtAsRuntimeException() {
		// Test that the exception can be caught as RuntimeException
		try {
			throw new PermitNotFoundException("Test message");
		} catch (RuntimeException e) {
			assertInstanceOf(PermitNotFoundException.class, e);
			assertEquals("Test message", e.getMessage());
		}
	}

	@Test
	void testExceptionCanBeCaughtAsPermitNotFoundException() {
		// Test that the exception can be caught as its specific type
		try {
			throw new PermitNotFoundException("Test message");
		} catch (PermitNotFoundException e) {
			assertEquals("Test message", e.getMessage());
		}
	}

	@Test
	void testWithDatabaseExceptionAsCause() {
		String message = "Permit lookup failed";
		Exception dbException = new org.springframework.dao.DataAccessException("Database connection failed") {
			@Override
			public String getMessage() {
				return "Database connection failed";
			}
		};

		PermitNotFoundException exception = new PermitNotFoundException(message, dbException);

		assertEquals(message, exception.getMessage());
		assertEquals(dbException, exception.getCause());
		assertEquals("Database connection failed", exception.getCause().getMessage());
	}

	@Test
	void testWithMultipleCauses() {
		String message = "Permit not found";
		RuntimeException rootCause = new RuntimeException("Root cause");
		RuntimeException intermediateCause = new RuntimeException("Intermediate cause", rootCause);
		PermitNotFoundException exception = new PermitNotFoundException(message, intermediateCause);

		assertEquals(message, exception.getMessage());
		assertEquals(intermediateCause, exception.getCause());
		assertEquals(rootCause, exception.getCause().getCause());
	}
}
