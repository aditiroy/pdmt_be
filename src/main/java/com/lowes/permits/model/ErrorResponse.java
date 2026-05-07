package com.lowes.permits.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
	private String type;
	private String message;
	private List<ValidationError> errors;
	private String xb3TraceId;
}
