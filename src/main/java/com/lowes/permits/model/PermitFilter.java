package com.lowes.permits.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PermitFilter {
	private List<String> cities;
	private List<String> zipCodes;
	private List<LaborCategory> laborCategories;
	private List<String> states;
	private List<String> counties;
	private List<String> municipalities;
	private List<PartialSearch> partialSearch;
}
