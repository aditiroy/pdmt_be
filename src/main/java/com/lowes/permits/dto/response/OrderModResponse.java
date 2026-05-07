package com.lowes.permits.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lowes.permits.model.Address;
import com.lowes.permits.model.Audit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderModResponse {
	private String id;
	private Integer laborCategoryCode;
	private String laborCategoryDescription;
	private Integer laborItem;
	private String laborItemDescription;
	private BigDecimal unitPermitFee;
	private String omniItemId;
	private Address address;
	private String provider;
	private String complianceStatus;
	private String status;
	private Integer vbuNumber;
	private LocalDateTime createdTimestamp;
	private String createdBy;
	private LocalDateTime lastUpdatedTimestamp;
	private String permitInsertType;
	private BigDecimal oldPermitFee;
	private String jobId;
	private String orderNumber;
	private Audit audit;
}
