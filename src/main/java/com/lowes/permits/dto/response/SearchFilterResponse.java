package com.lowes.permits.dto.response;

import java.util.List;

import lombok.Data;

@Data
public class SearchFilterResponse {

	private List<SearchFilterResponseData> data;

	@Data
	public static class SearchFilterResponseData {
		private List<String> values;
		private String type;
		private Integer count;
	}
}
