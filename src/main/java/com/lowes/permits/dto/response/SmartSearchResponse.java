package com.lowes.permits.dto.response;

import java.util.List;

import lombok.Data;

@Data
public class SmartSearchResponse {
	List<SmartSearchResult> data;

	@Data
	public static class SmartSearchResult {
		private String type;
		private String value;
		private String code;
	}
}
