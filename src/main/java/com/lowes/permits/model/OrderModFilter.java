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
public class OrderModFilter {
	private List<String> cities;
	private List<String> zipCodes;
	private List<LaborCategory> laborCategories;
	private List<String> states;
	private List<String> counties;
	private List<String> municipalities;
	private List<String> complianceStatuses;
	private List<String> statuses;
	private List<String> permitInsertTypes;
	private List<String> orderNumbers;
	private List<String> jobIds;
	private List<String> providers;
	private List<PartialSearch> partialSearch;
}
