package com.lowes.permits.dto.response;

import java.math.BigDecimal;

import com.lowes.permits.model.Address;
import com.lowes.permits.model.Audit;
import com.lowes.permits.model.LaborCategory;
import com.lowes.permits.model.Provider;

import lombok.Data;

@Data
public class PermitResponse {
	private String id;
	private Integer laborItem;
	private String laborItemDescription;
	private BigDecimal unitPermitFee;
	private String omniItemId;
	private LaborCategory laborCategory;
	private Address address;
	private Provider provider;
	private Audit audit;
	private Integer estPermitObtainDays;
}
