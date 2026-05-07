package com.lowes.permits.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lowes.permits.model.OperationType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EditActivityRequest {

	@NotNull(message = "operationType is required")
	private OperationType operationType;

	@Valid
	private CreatePermitRequest createRequest;

	@Valid
	private EditActivityFeeRequest updateRequest;

	@AssertTrue(message = "createRequest is required for CREATE operation")
	public boolean isCreatePayloadValid() {
		if (operationType == null) {
			return true;
		}
		if (OperationType.CREATE.equals(operationType)) {
			return createRequest != null;
		}
		return true;
	}

	@AssertTrue(message = "updateRequest is required for UPDATE operation")
	public boolean isUpdatePayloadValid() {
		if (operationType == null) {
			return true;
		}
		if (OperationType.UPDATE.equals(operationType)) {
			return updateRequest != null;
		}
		return true;
	}
}
