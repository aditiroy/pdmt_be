package com.lowes.permits.dto.request;

import java.math.BigDecimal;

import com.lowes.permits.model.Address;
import com.lowes.permits.model.LaborCategory;
import com.lowes.permits.model.Provider;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePermitRequest {
	@NotNull(message = "laborItem is required")
	private Integer laborItem;

	@NotBlank(message = "laborItemDescription is required")
	private String laborItemDescription;

	@NotNull(message = "unitPermitFee is required")
	private BigDecimal unitPermitFee;

	private Integer estPermitObtainDays;

	@NotBlank(message = "omniItemId is required")
	private String omniItemId;

	@NotNull(message = "laborCategory is required")
	@Valid
	private LaborCategory laborCategory;

	@NotNull(message = "address is required")
	@Valid
	private Address address;

	@Valid
	private Provider provider;
}
