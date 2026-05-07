package com.lowes.permits.dto.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateActivityRequest {

	@Valid
	private CreatePermitRequest createRequest;

	@Valid
	private List<UpdatePermitRequest> updateRequest;

	@Valid
	private List<DeletePermitRequest> deleteRequest;

	@Valid
	private ApproveRejectOrderModRequest approveRequest;

	@Valid
	private ApproveRejectOrderModRequest rejectRequest;
}
