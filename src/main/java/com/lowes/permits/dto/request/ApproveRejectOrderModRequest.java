package com.lowes.permits.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApproveRejectOrderModRequest {

	@NotEmpty(message = "ids must not be empty")
	private List<String> ids;
}
