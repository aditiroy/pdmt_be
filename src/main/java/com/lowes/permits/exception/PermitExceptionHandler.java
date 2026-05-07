package com.lowes.permits.exception;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.core.codec.DecodingException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.MissingRequestValueException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.lowes.permits.constants.ApplicationConstants;
import com.lowes.permits.model.ErrorResponse;
import com.lowes.permits.model.ValidationError;
import com.lowes.permits.util.PermitUtils;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice(basePackages = "com.lowes.permits")
@Order(-1)
public class PermitExceptionHandler {

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, ServerWebExchange exchange) {
		String traceId = PermitUtils.resolveTraceId(
				exchange.getRequest().getHeaders().getFirst(ApplicationConstants.X_B3_TRACE_ID));
		log.error("Unexpected error while creating permit, xb3TraceId={}", traceId, ex);

		ErrorResponse error = new ErrorResponse(
				"InternalServerError", "An unexpected error occurred while creating permit", null, traceId);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
	}

	@ExceptionHandler(WebExchangeBindException.class)
	public ResponseEntity<ErrorResponse> handleValidationException(
			WebExchangeBindException ex, ServerWebExchange exchange) {
		String traceId = PermitUtils.resolveTraceId(
				exchange.getRequest().getHeaders().getFirst(ApplicationConstants.X_B3_TRACE_ID));
		log.warn("Validation error, xb3TraceId={}", traceId);

		List<ValidationError> errors = ex.getFieldErrors().stream()
				.map(fieldError -> new ValidationError(fieldError.getDefaultMessage()))
				.toList();

		ErrorResponse response = new ErrorResponse(
				"ValidationError", "Request validation failed. Missing required field(s)", errors, traceId);

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErrorResponse> handleConstraintViolationException(
			ConstraintViolationException ex, ServerWebExchange exchange) {
		String traceId = PermitUtils.resolveTraceId(
				exchange.getRequest().getHeaders().getFirst(ApplicationConstants.X_B3_TRACE_ID));
		log.error("Exception Occurred : ConstraintViolationException - {}", ex);

		List<ValidationError> errors = ex.getConstraintViolations().stream()
				.map(violation -> new ValidationError(violation.getMessage()))
				.toList();

		ErrorResponse response = new ErrorResponse(
				"ValidationError", "Request validation failed. Missing required field(s)", errors, traceId);

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}

	@ExceptionHandler(DuplicateKeyException.class)
	public ResponseEntity<ErrorResponse> handleDuplicateKeyException(
			DuplicateKeyException ex, ServerWebExchange exchange) {
		String traceId = PermitUtils.resolveTraceId(
				exchange.getRequest().getHeaders().getFirst(ApplicationConstants.X_B3_TRACE_ID));

		String permitDbId = extractPermitDbIdFromException(ex);
		log.warn("Duplicate permit for permitDbId={}, xb3TraceId={}", permitDbId, traceId);

		ErrorResponse error = new ErrorResponse(
				"DuplicateResource",
				"Permit already exists for given permitDbId:" + permitDbId + ", status, and operationType combination",
				List.of(new ValidationError(
						"Duplicate permit for the given composite key (permitDbId, status, operationType)")),
				traceId);
		return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
	}

	private String extractPermitDbIdFromException(DuplicateKeyException ex) {
		String message = ex.getMessage();
		if (message != null && message.contains("permitDbId")) {
			try {
				int startIndex = message.indexOf("permitDbId") + "permitDbId".length();
				String substring = message.substring(startIndex);
				int colonIndex = substring.indexOf(":");
				int commaIndex = substring.indexOf(",");
				int endIndex = commaIndex != -1 ? commaIndex : substring.length();

				if (colonIndex != -1 && colonIndex < endIndex) {
					String value = substring.substring(colonIndex + 1, endIndex).trim();
					return value.replaceAll("[\"'\\s]", "");
				}
			} catch (Exception e) {
				log.debug("Could not extract permitDbId from exception message", e);
			}
		}
		return "";
	}

	@ExceptionHandler(PermitMainDbConflictException.class)
	public ResponseEntity<ErrorResponse> handlePermitMainDbConflictException(
			PermitMainDbConflictException ex, ServerWebExchange exchange) {
		String traceId = PermitUtils.resolveTraceId(
				exchange.getRequest().getHeaders().getFirst(ApplicationConstants.X_B3_TRACE_ID));
		log.warn("Permit already exists in main DB, xb3TraceId={}", traceId);

		ErrorResponse error = new ErrorResponse(
				"DuplicateInMainDB",
				ex.getMessage(),
				List.of(new ValidationError("Permit already exists in permits main DB")),
				traceId);
		return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
	}

	@ExceptionHandler(InvalidUserTokenException.class)
	public ResponseEntity<ErrorResponse> handleInvalidUserTokenException(
			InvalidUserTokenException ex, ServerWebExchange exchange) {
		String traceId = PermitUtils.resolveTraceId(
				exchange.getRequest().getHeaders().getFirst(ApplicationConstants.X_B3_TRACE_ID));
		log.warn("Invalid X-USER-TOKEN header, xb3TraceId={}", traceId);

		List<ValidationError> errors = List.of(new ValidationError("The X-USER-TOKEN header value is invalid"));
		ErrorResponse error = new ErrorResponse("InvalidUserToken", "Invalid X-USER-TOKEN header", errors, traceId);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}

	@ExceptionHandler(PermitNotFoundException.class)
	public ResponseEntity<ErrorResponse> handlePermitNotFoundException(
			PermitNotFoundException ex, ServerWebExchange exchange) {
		String traceId = PermitUtils.resolveTraceId(
				exchange.getRequest().getHeaders().getFirst(ApplicationConstants.X_B3_TRACE_ID));
		log.warn("Permit not found, xb3TraceId={}", traceId);

		List<ValidationError> errors = List.of(new ValidationError(ex.getMessage()));
		ErrorResponse error =
				new ErrorResponse("ResourceNotFound", "Permit not found for the given permitDbId", errors, traceId);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
	}

	@ExceptionHandler(MissingRequestValueException.class)
	public ResponseEntity<ErrorResponse> handleMissingRequestValueException(
			MissingRequestValueException ex, ServerWebExchange exchange) {
		String traceId = PermitUtils.resolveTraceId(
				exchange.getRequest().getHeaders().getFirst(ApplicationConstants.X_B3_TRACE_ID));
		log.warn("Missing required request value, xb3TraceId={}", traceId);

		List<ValidationError> errors = List.of(new ValidationError(ex.getReason()));

		ErrorResponse error = new ErrorResponse(
				"ValidationError", "Request validation failed. Missing required field(s)", errors, traceId);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}

	@ExceptionHandler(ServerWebInputException.class)
	public ResponseEntity<ErrorResponse> handleServerWebInputException(
			ServerWebInputException ex, ServerWebExchange exchange) {
		String traceId = PermitUtils.resolveTraceId(
				exchange.getRequest().getHeaders().getFirst(ApplicationConstants.X_B3_TRACE_ID));
		log.warn("Invalid request body format, xb3TraceId={}", traceId);

		String message = "Invalid request body format";
		if (ex.getCause() instanceof DecodingException decodingException) {
			message = extractFieldErrorMessage(decodingException);
		}

		List<ValidationError> errors = List.of(new ValidationError(message));
		ErrorResponse error =
				new ErrorResponse("InvalidRequestBody", "Request body contains invalid data type(s)", errors, traceId);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
			IllegalArgumentException ex, ServerWebExchange exchange) {
		String traceId = PermitUtils.resolveTraceId(
				exchange.getRequest().getHeaders().getFirst(ApplicationConstants.X_B3_TRACE_ID));
		log.warn("Invalid argument provided, xb3TraceId={}", traceId);

		List<ValidationError> errors = List.of(new ValidationError(ex.getMessage()));
		ErrorResponse error = new ErrorResponse("ValidationError", "Invalid request parameter", errors, traceId);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}

	@ExceptionHandler(PermitSearchException.class)
	public ResponseEntity<ErrorResponse> handle(PermitSearchException ex, ServerWebExchange exchange) {
		String traceId = PermitUtils.resolveTraceId(
				exchange.getRequest().getHeaders().getFirst(ApplicationConstants.X_B3_TRACE_ID));
		log.error("Unexpected error while searching permit, xb3TraceId={}", traceId, ex);

		ErrorResponse error = new ErrorResponse("InternalServerError", ex.getMessage(), null, traceId);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
	}

	@ExceptionHandler(PermitExportNotFoundException.class)
	public ResponseEntity<ErrorResponse> handlePermitExportNotFoundException(
			PermitExportNotFoundException ex, ServerWebExchange exchange) {
		String traceId = PermitUtils.resolveTraceId(
				exchange.getRequest().getHeaders().getFirst(ApplicationConstants.X_B3_TRACE_ID));
		log.warn("Permit export not found, xb3TraceId={}", traceId);

		List<ValidationError> errors = List.of(new ValidationError(ex.getMessage()));
		ErrorResponse error = new ErrorResponse("ResourceNotFound", "Permit file not found", errors, traceId);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
	}

	private String extractFieldErrorMessage(DecodingException ex) {
		Throwable cause = ex.getCause();
		if (cause instanceof InvalidFormatException invalidFormatEx) {
			String fieldName = extractFieldName(invalidFormatEx);
			String fieldType = extractFieldType(invalidFormatEx.getTargetType());
			if (fieldName != null) {
				return "Invalid value provided for '" + fieldName + "'. Expected a valid " + fieldType;
			}
			return "Invalid value provided. Expected a valid " + fieldType;
		}
		return "Invalid data type in request body";
	}

	private String extractFieldName(InvalidFormatException invalidFormatEx) {
		var path = invalidFormatEx.getPath();
		if (path != null && !path.isEmpty()) {
			StringBuilder fieldPath = new StringBuilder();
			for (int i = 0; i < path.size(); i++) {
				if (i > 0) fieldPath.append(".");
				fieldPath.append(path.get(i).getFieldName());
			}
			return fieldPath.toString();
		}
		return null;
	}

	private String extractFieldType(Class<?> targetType) {
		if (targetType == null) {
			return "value";
		}
		if (Integer.class.equals(targetType)
				|| Long.class.equals(targetType)
				|| int.class.equals(targetType)
				|| long.class.equals(targetType)) {
			return "integer value";
		} else if (Double.class.equals(targetType)
				|| Float.class.equals(targetType)
				|| BigDecimal.class.equals(targetType)
				|| double.class.equals(targetType)
				|| float.class.equals(targetType)) {
			return "decimal value";
		} else if (Boolean.class.equals(targetType) || boolean.class.equals(targetType)) {
			return "boolean value (true/false)";
		}
		return "value";
	}
}
