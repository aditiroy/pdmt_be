package com.lowes.permits.dto.response;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lowes.permits.model.Address;
import com.lowes.permits.model.Audit;
import com.lowes.permits.model.LaborCategory;
import com.lowes.permits.model.OperationType;
import com.lowes.permits.model.PermitStatus;
import com.lowes.permits.model.Provider;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ActivityResponse {
	private String id;
	private String permitDbId;
	private PermitStatus status;
	private OperationType operationType;
	private BigDecimal unitPermitFee;
	private BigDecimal oldUnitPermitFee;
	private Integer estPermitObtainDays;
	private Integer oldEstPermitObtainDays;
	private Integer laborItem;
	private String laborItemDescription;
	private String omniItemId;
	private LaborCategory laborCategory;
	private Address address;
	private Audit audit;
	private Provider provider;
}
