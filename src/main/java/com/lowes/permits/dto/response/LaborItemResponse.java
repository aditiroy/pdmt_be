package com.lowes.permits.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LaborItemResponse {

	String laborItem;
	String laborItemDescription;
	String omniItemId;
	String errorMessage;
	LaborCategory laborCategory;

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class LaborCategory {
		private Integer code;
		private String description;
	}
}
