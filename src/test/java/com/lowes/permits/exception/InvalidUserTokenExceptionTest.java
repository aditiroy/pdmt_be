package com.lowes.permits.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InvalidUserTokenExceptionTest {

	@Test
	void testConstructorWithMessage() {
		String message = "Invalid user token provided";
		InvalidUserTokenException exception = new InvalidUserTokenException(message);

		assertEquals(message, exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	void testConstructorWithMessageAndCause() {
		String message = "Invalid user token provided";
		Throwable cause = new RuntimeException("Underlying cause");
		InvalidUserTokenException exception = new InvalidUserTokenException(message, cause);

		assertEquals(message, exception.getMessage());
		assertEquals(cause, exception.getCause());
	}

	@Test
	void testInheritanceFromRuntimeException() {
		InvalidUserTokenException exception = new InvalidUserTokenException("test message");

		assertInstanceOf(RuntimeException.class, exception);
		assertInstanceOf(RuntimeException.class, exception);
	}

	@Test
	void testWithNullMessage() {
		InvalidUserTokenException exception = new InvalidUserTokenException(null);

		assertNull(exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	void testWithEmptyMessage() {
		String emptyMessage = "";
		InvalidUserTokenException exception = new InvalidUserTokenException(emptyMessage);

		assertEquals(emptyMessage, exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	void testWithNullCause() {
		String message = "Test message";
		InvalidUserTokenException exception = new InvalidUserTokenException(message, null);

		assertEquals(message, exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	void testWithComplexMessage() {
		String complexMessage =
				"Invalid user token: 'abc123' does not match expected format. Token must be alphanumeric and at least 8 characters long.";
		InvalidUserTokenException exception = new InvalidUserTokenException(complexMessage);

		assertEquals(complexMessage, exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	void testWithNestedCause() {
		String message = "Token validation failed";
		IllegalArgumentException nestedCause = new IllegalArgumentException("Token format is invalid");
		InvalidUserTokenException exception = new InvalidUserTokenException(message, nestedCause);

		assertEquals(message, exception.getMessage());
		assertEquals(nestedCause, exception.getCause());
		assertInstanceOf(IllegalArgumentException.class, nestedCause);
	}

	@Test
	void testExceptionChaining() {
		String message = "User token validation error";
		RuntimeException rootCause = new RuntimeException("Root cause");
		InvalidUserTokenException exception = new InvalidUserTokenException(message, rootCause);

		// Test exception chaining
		assertEquals(rootCause, exception.getCause());
		assertEquals("Root cause", exception.getCause().getMessage());
	}

	@Test
	void testStackTrace() {
		String message = "Test exception";
		InvalidUserTokenException exception = new InvalidUserTokenException(message);

		// Verify that stack trace is available
		assertNotNull(exception.getStackTrace());
		assertTrue(exception.getStackTrace().length > 0);
	}

	@Test
	void testToString() {
		String message = "Test message";
		InvalidUserTokenException exception = new InvalidUserTokenException(message);

		String toString = exception.toString();

		assertTrue(toString.contains("InvalidUserTokenException"));
		assertTrue(toString.contains(message));
	}

	@Test
	void testWithSpecialCharactersInMessage() {
		String specialMessage = "Invalid token with special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?";
		InvalidUserTokenException exception = new InvalidUserTokenException(specialMessage);

		assertEquals(specialMessage, exception.getMessage());
	}

	@Test
	void testWithUnicodeInMessage() {
		String unicodeMessage = "Token inválido: ñáéíóú 中文 العربية";
		InvalidUserTokenException exception = new InvalidUserTokenException(unicodeMessage);

		assertEquals(unicodeMessage, exception.getMessage());
	}

	@Test
	void testWithLongMessage() {
		StringBuilder longMessageBuilder = new StringBuilder();
		for (int i = 0; i < 1000; i++) {
			longMessageBuilder
					.append("This is a very long message part ")
					.append(i)
					.append(". ");
		}
		String longMessage = longMessageBuilder.toString();

		InvalidUserTokenException exception = new InvalidUserTokenException(longMessage);

		assertEquals(longMessage, exception.getMessage());
		assertTrue(longMessage.length() > 1000);
	}

	@Test
	void testExceptionCanBeCaughtAsRuntimeException() {
		// Test that the exception can be caught as RuntimeException
		try {
			throw new InvalidUserTokenException("Test message");
		} catch (RuntimeException e) {
			assertInstanceOf(InvalidUserTokenException.class, e);
			assertEquals("Test message", e.getMessage());
		}
	}

	@Test
	void testExceptionCanBeCaughtAsInvalidUserTokenException() {
		// Test that the exception can be caught as its specific type
		try {
			throw new InvalidUserTokenException("Test message");
		} catch (InvalidUserTokenException e) {
			assertEquals("Test message", e.getMessage());
		}
	}
}
