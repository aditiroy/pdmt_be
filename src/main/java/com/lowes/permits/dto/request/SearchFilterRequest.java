package com.lowes.permits.dto.request;

import java.util.List;

import lombok.Data;

@Data
public class SearchFilterRequest {

	private List<Filter> filters;

	@Data
	public static class Filter {
		private String type;
		private List<String> values;
	}
}
