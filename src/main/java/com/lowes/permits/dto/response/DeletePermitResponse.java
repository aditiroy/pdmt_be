package com.lowes.permits.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeletePermitResponse {
	private String message;
	private List<String> existingPermitDbIds;
}
