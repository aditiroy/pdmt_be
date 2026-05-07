package com.lowes.permits.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermitCsvRow {
	private String laborCategoryCode;
	private String laborCategoryDescription;
	private String zipcode;
	private String city;
	private String state;
	private String laborItem;
	private String laborItemDescription;
	private String unitPermitFee;
	private String omniItemId;
	private String provider;
	private String vbuNumber;
	private String createdBy;
	private String createdTimestamp;
	private String updatedBy;
	private String updatedTimestamp;
	private String oldPrice;
	private String county;
	private String municipality;
	private String estPermitObtainDays;
}
