package com.lowes.permits.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApproveRejectOrderModResponse {
	private String message;
	private List<String> nonEligibleIds;
}
