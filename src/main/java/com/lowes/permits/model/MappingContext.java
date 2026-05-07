package com.lowes.permits.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MappingContext {
	private String xUserToken;
	private String currentTraceId;
	private String callerApp;
	private OperationType operationType;
	private PermitStatus status;
}
