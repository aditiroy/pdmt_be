package com.lowes.permits.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.MissingRequestValueException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

import com.lowes.permits.model.ErrorResponse;
import com.lowes.permits.util.PermitUtils;

import jakarta.validation.ConstraintViolationException;

@ExtendWith(MockitoExtension.class)
class PermitPostgresEntityExceptionHandlerTest {

	@InjectMocks
	private PermitExceptionHandler permitExceptionHandler;

	@Mock
	private ServerWebExchange exchange;

	@BeforeEach
	void setUp() {
		// Mock the exchange to return headers
		when(exchange.getRequest()).thenReturn(MockServerHttpRequest.get("").build());
	}

	@Test
	void testHandleGenericException() {
		Exception ex = new RuntimeException("Test exception");

		try (MockedStatic<PermitUtils> permitUtilsMock = mockStatic(PermitUtils.class)) {
			permitUtilsMock.when(() -> PermitUtils.resolveTraceId(any())).thenReturn("trace123");

			ResponseEntity<ErrorResponse> response = permitExceptionHandler.handleGenericException(ex, exchange);

			assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
			ErrorResponse errorResponse = response.getBody();
			assertNotNull(errorResponse);
			assertEquals("InternalServerError", errorResponse.getType());
			assertEquals("An unexpected error occurred while creating permit", errorResponse.getMessage());
			assertEquals("trace123", errorResponse.getXb3TraceId());
			assertNull(errorResponse.getErrors());
		}
	}

	@Test
	void testHandleValidationException() {
		WebExchangeBindException ex = mock(WebExchangeBindException.class);
		org.springframework.validation.FieldError fieldError = new org.springframework.validation.FieldError(
				"object", "fieldName", "rejectedValue", false, null, null, "Field is required");

		when(ex.getFieldErrors()).thenReturn(List.of(fieldError));

		try (MockedStatic<PermitUtils> permitUtilsMock = mockStatic(PermitUtils.class)) {
			permitUtilsMock.when(() -> PermitUtils.resolveTraceId(any())).thenReturn("trace123");

			ResponseEntity<ErrorResponse> response = permitExceptionHandler.handleValidationException(ex, exchange);

			assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
			ErrorResponse errorResponse = response.getBody();
			assertNotNull(errorResponse);
			assertEquals("ValidationError", errorResponse.getType());
			assertEquals("Request validation failed. Missing required field(s)", errorResponse.getMessage());
			assertEquals("trace123", errorResponse.getXb3TraceId());
			assertNotNull(errorResponse.getErrors());
			assertEquals(1, errorResponse.getErrors().size());
			assertEquals("Field is required", errorResponse.getErrors().get(0).getMessage());
		}
	}

	@Test
	void testHandleConstraintViolationException() {
		jakarta.validation.ConstraintViolation<?> violation = mock(jakarta.validation.ConstraintViolation.class);
		when(violation.getMessage()).thenReturn("Constraint violation message");

		ConstraintViolationException ex = new ConstraintViolationException("Constraint violation", Set.of(violation));

		try (MockedStatic<PermitUtils> permitUtilsMock = mockStatic(PermitUtils.class)) {
			permitUtilsMock.when(() -> PermitUtils.resolveTraceId(any())).thenReturn("trace123");

			ResponseEntity<ErrorResponse> response =
					permitExceptionHandler.handleConstraintViolationException(ex, exchange);

			assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
			ErrorResponse errorResponse = response.getBody();
			assertNotNull(errorResponse);
			assertEquals("ValidationError", errorResponse.getType());
			assertEquals("Request validation failed. Missing required field(s)", errorResponse.getMessage());
			assertEquals("trace123", errorResponse.getXb3TraceId());
			assertNotNull(errorResponse.getErrors());
			assertEquals(1, errorResponse.getErrors().size());
			assertEquals(
					"Constraint violation message",
					errorResponse.getErrors().get(0).getMessage());
		}
	}

	@Test
	void testHandleDuplicateKeyException() {
		DuplicateKeyException ex =
				new DuplicateKeyException("Duplicate key error with permitDbId:\"PERMIT123\", status:\"NEW\"");

		try (MockedStatic<PermitUtils> permitUtilsMock = mockStatic(PermitUtils.class)) {
			permitUtilsMock.when(() -> PermitUtils.resolveTraceId(any())).thenReturn("trace123");

			ResponseEntity<ErrorResponse> response = permitExceptionHandler.handleDuplicateKeyException(ex, exchange);

			assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
			ErrorResponse errorResponse = response.getBody();
			assertNotNull(errorResponse);
			assertEquals("DuplicateResource", errorResponse.getType());
			assertTrue(errorResponse.getMessage().contains("Permit already exists for given permitDbId:PERMIT123"));
			assertEquals("trace123", errorResponse.getXb3TraceId());
			assertNotNull(errorResponse.getErrors());
			assertEquals(1, errorResponse.getErrors().size());
			assertTrue(errorResponse
					.getErrors()
					.get(0)
					.getMessage()
					.contains("Duplicate permit for the given composite key"));
		}
	}

	@Test
	void testHandleInvalidUserTokenException() {
		InvalidUserTokenException ex = new InvalidUserTokenException("Invalid token");

		try (MockedStatic<PermitUtils> permitUtilsMock = mockStatic(PermitUtils.class)) {
			permitUtilsMock.when(() -> PermitUtils.resolveTraceId(any())).thenReturn("trace123");

			ResponseEntity<ErrorResponse> response =
					permitExceptionHandler.handleInvalidUserTokenException(ex, exchange);

			assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
			ErrorResponse errorResponse = response.getBody();
			assertNotNull(errorResponse);
			assertEquals("InvalidUserToken", errorResponse.getType());
			assertEquals("Invalid X-USER-TOKEN header", errorResponse.getMessage());
			assertEquals("trace123", errorResponse.getXb3TraceId());
			assertNotNull(errorResponse.getErrors());
			assertEquals(1, errorResponse.getErrors().size());
			assertEquals(
					"The X-USER-TOKEN header value is invalid",
					errorResponse.getErrors().get(0).getMessage());
		}
	}

	@Test
	void testHandlePermitNotFoundException() {
		PermitNotFoundException ex = new PermitNotFoundException("Permit not found");

		try (MockedStatic<PermitUtils> permitUtilsMock = mockStatic(PermitUtils.class)) {
			permitUtilsMock.when(() -> PermitUtils.resolveTraceId(any())).thenReturn("trace123");

			ResponseEntity<ErrorResponse> response = permitExceptionHandler.handlePermitNotFoundException(ex, exchange);

			assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
			ErrorResponse errorResponse = response.getBody();
			assertNotNull(errorResponse);
			assertEquals("ResourceNotFound", errorResponse.getType());
			assertEquals("Permit not found for the given permitDbId", errorResponse.getMessage());
			assertEquals("trace123", errorResponse.getXb3TraceId());
			assertNotNull(errorResponse.getErrors());
			assertEquals(1, errorResponse.getErrors().size());
			assertEquals("Permit not found", errorResponse.getErrors().get(0).getMessage());
		}
	}

	@Test
	void testHandleMissingRequestValueException() {
		MissingRequestValueException ex = mock(MissingRequestValueException.class);
		when(ex.getReason()).thenReturn("Missing required parameter");

		try (MockedStatic<PermitUtils> permitUtilsMock = mockStatic(PermitUtils.class)) {
			permitUtilsMock.when(() -> PermitUtils.resolveTraceId(any())).thenReturn("trace123");

			ResponseEntity<ErrorResponse> response =
					permitExceptionHandler.handleMissingRequestValueException(ex, exchange);

			assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
			ErrorResponse errorResponse = response.getBody();
			assertNotNull(errorResponse);
			assertEquals("ValidationError", errorResponse.getType());
			assertEquals("Request validation failed. Missing required field(s)", errorResponse.getMessage());
			assertEquals("trace123", errorResponse.getXb3TraceId());
			assertNotNull(errorResponse.getErrors());
			assertEquals(1, errorResponse.getErrors().size());
			assertEquals(
					"Missing required parameter",
					errorResponse.getErrors().get(0).getMessage());
		}
	}

	@Test
	void testHandleServerWebInputException() {
		ServerWebInputException ex = new ServerWebInputException("Invalid input");

		try (MockedStatic<PermitUtils> permitUtilsMock = mockStatic(PermitUtils.class)) {
			permitUtilsMock.when(() -> PermitUtils.resolveTraceId(any())).thenReturn("trace123");

			ResponseEntity<ErrorResponse> response = permitExceptionHandler.handleServerWebInputException(ex, exchange);

			assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
			ErrorResponse errorResponse = response.getBody();
			assertNotNull(errorResponse);
			assertEquals("InvalidRequestBody", errorResponse.getType());
			assertEquals("Request body contains invalid data type(s)", errorResponse.getMessage());
			assertEquals("trace123", errorResponse.getXb3TraceId());
			assertNotNull(errorResponse.getErrors());
			assertEquals(1, errorResponse.getErrors().size());
			assertEquals(
					"Invalid request body format",
					errorResponse.getErrors().get(0).getMessage());
		}
	}

	@Test
	void testHandleIllegalArgumentException() {
		IllegalArgumentException ex = new IllegalArgumentException("Invalid argument");

		try (MockedStatic<PermitUtils> permitUtilsMock = mockStatic(PermitUtils.class)) {
			permitUtilsMock.when(() -> PermitUtils.resolveTraceId(any())).thenReturn("trace123");

			ResponseEntity<ErrorResponse> response =
					permitExceptionHandler.handleIllegalArgumentException(ex, exchange);

			assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
			ErrorResponse errorResponse = response.getBody();
			assertNotNull(errorResponse);
			assertEquals("ValidationError", errorResponse.getType());
			assertEquals("Invalid request parameter", errorResponse.getMessage());
			assertEquals("trace123", errorResponse.getXb3TraceId());
			assertNotNull(errorResponse.getErrors());
			assertEquals(1, errorResponse.getErrors().size());
			assertEquals("Invalid argument", errorResponse.getErrors().get(0).getMessage());
		}
	}

	@Test
	void testHandlePermitSearchException() {
		PermitSearchException ex = new PermitSearchException("Search error");

		try (MockedStatic<PermitUtils> permitUtilsMock = mockStatic(PermitUtils.class)) {
			permitUtilsMock.when(() -> PermitUtils.resolveTraceId(any())).thenReturn("trace123");

			ResponseEntity<ErrorResponse> response = permitExceptionHandler.handle(ex, exchange);

			assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
			ErrorResponse errorResponse = response.getBody();
			assertNotNull(errorResponse);
			assertEquals("InternalServerError", errorResponse.getType());
			assertEquals("Search error", errorResponse.getMessage());
			assertEquals("trace123", errorResponse.getXb3TraceId());
			assertNull(errorResponse.getErrors());
		}
	}

	@Test
	void testHandlePermitExportNotFoundException() {
		PermitExportNotFoundException ex =
				new PermitExportNotFoundException("No permit report data found. Please generate the report first.");

		try (MockedStatic<PermitUtils> permitUtilsMock = mockStatic(PermitUtils.class)) {
			permitUtilsMock.when(() -> PermitUtils.resolveTraceId(any())).thenReturn("trace123");

			ResponseEntity<ErrorResponse> response =
					permitExceptionHandler.handlePermitExportNotFoundException(ex, exchange);

			assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
			ErrorResponse errorResponse = response.getBody();
			assertNotNull(errorResponse);
			assertEquals("ResourceNotFound", errorResponse.getType());
			assertEquals("Permit file not found", errorResponse.getMessage());
			assertEquals("trace123", errorResponse.getXb3TraceId());
			assertNotNull(errorResponse.getErrors());
			assertEquals(1, errorResponse.getErrors().size());
			assertEquals(
					"No permit report data found. Please generate the report first.",
					errorResponse.getErrors().get(0).getMessage());
		}
	}

	@Test
	void testHandlePermitExportNotFoundException_S3FileNotFound() {
		PermitExportNotFoundException ex = new PermitExportNotFoundException(
				"Permit file not found in storage. The file may have been deleted or moved.");

		try (MockedStatic<PermitUtils> permitUtilsMock = mockStatic(PermitUtils.class)) {
			permitUtilsMock.when(() -> PermitUtils.resolveTraceId(any())).thenReturn("trace123");

			ResponseEntity<ErrorResponse> response =
					permitExceptionHandler.handlePermitExportNotFoundException(ex, exchange);

			assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
			ErrorResponse errorResponse = response.getBody();
			assertNotNull(errorResponse);
			assertEquals("ResourceNotFound", errorResponse.getType());
			assertEquals("Permit file not found", errorResponse.getMessage());
			assertEquals("trace123", errorResponse.getXb3TraceId());
			assertNotNull(errorResponse.getErrors());
			assertEquals(1, errorResponse.getErrors().size());
			assertEquals(
					"Permit file not found in storage. The file may have been deleted or moved.",
					errorResponse.getErrors().get(0).getMessage());
		}
	}

	@Test
	void testHandleDuplicateKeyExceptionWithNullMessage() {
		// Test handleDuplicateKeyException with null message
		DuplicateKeyException ex = new DuplicateKeyException(null);

		try (MockedStatic<PermitUtils> permitUtilsMock = mockStatic(PermitUtils.class)) {
			permitUtilsMock.when(() -> PermitUtils.resolveTraceId(any())).thenReturn("trace123");

			ResponseEntity<ErrorResponse> response = permitExceptionHandler.handleDuplicateKeyException(ex, exchange);

			assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
			ErrorResponse errorResponse = response.getBody();
			assertNotNull(errorResponse);
			assertEquals("DuplicateResource", errorResponse.getType());
			assertTrue(errorResponse.getMessage().contains("Permit already exists for given permitDbId:"));
			assertEquals("trace123", errorResponse.getXb3TraceId());
			assertNotNull(errorResponse.getErrors());
			assertEquals(1, errorResponse.getErrors().size());
		}
	}

	@Test
	void testHandleValidationExceptionWithMultipleFieldErrors() {
		// Test handleValidationException with multiple field errors
		WebExchangeBindException ex = mock(WebExchangeBindException.class);
		org.springframework.validation.FieldError fieldError1 = new org.springframework.validation.FieldError(
				"object", "field1", "rejectedValue1", false, null, null, "Field1 is required");
		org.springframework.validation.FieldError fieldError2 = new org.springframework.validation.FieldError(
				"object", "field2", "rejectedValue2", false, null, null, "Field2 is required");

		when(ex.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

		try (MockedStatic<PermitUtils> permitUtilsMock = mockStatic(PermitUtils.class)) {
			permitUtilsMock.when(() -> PermitUtils.resolveTraceId(any())).thenReturn("trace123");

			ResponseEntity<ErrorResponse> response = permitExceptionHandler.handleValidationException(ex, exchange);

			assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
			ErrorResponse errorResponse = response.getBody();
			assertNotNull(errorResponse);
			assertEquals("ValidationError", errorResponse.getType());
			assertEquals("Request validation failed. Missing required field(s)", errorResponse.getMessage());
			assertEquals("trace123", errorResponse.getXb3TraceId());
			assertNotNull(errorResponse.getErrors());
			assertEquals(2, errorResponse.getErrors().size());
			assertEquals("Field1 is required", errorResponse.getErrors().get(0).getMessage());
			assertEquals("Field2 is required", errorResponse.getErrors().get(1).getMessage());
		}
	}

	@Test
	void testHandleConstraintViolationExceptionWithMultipleViolations() {
		// Test handleConstraintViolationException with multiple violations
		jakarta.validation.ConstraintViolation<String> violation1 = mock(jakarta.validation.ConstraintViolation.class);
		when(violation1.getMessage()).thenReturn("Constraint violation 1");
		jakarta.validation.ConstraintViolation<String> violation2 = mock(jakarta.validation.ConstraintViolation.class);
		when(violation2.getMessage()).thenReturn("Constraint violation 2");

		// Mock the ConstraintViolationException to avoid constructor validation issues
		ConstraintViolationException ex = mock(ConstraintViolationException.class);
		// Use a List to maintain order instead of Set
		when(ex.getConstraintViolations()).thenReturn(Set.of(violation1, violation2));

		try (MockedStatic<PermitUtils> permitUtilsMock = mockStatic(PermitUtils.class)) {
			permitUtilsMock.when(() -> PermitUtils.resolveTraceId(any())).thenReturn("trace123");

			ResponseEntity<ErrorResponse> response =
					permitExceptionHandler.handleConstraintViolationException(ex, exchange);

			assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
			ErrorResponse errorResponse = response.getBody();
			assertNotNull(errorResponse);
			assertEquals("ValidationError", errorResponse.getType());
			assertEquals("Request validation failed. Missing required field(s)", errorResponse.getMessage());
			assertEquals("trace123", errorResponse.getXb3TraceId());
			assertNotNull(errorResponse.getErrors());
			assertEquals(2, errorResponse.getErrors().size());
		}
	}
}
