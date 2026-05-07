package com.lowes.permits.dto.request;

import java.math.BigDecimal;

import com.lowes.permits.model.LaborCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DeletePermitRequest {
	@NotBlank(message = "permitDbId must not be blank")
	private String permitDbId;

	@NotNull(message = "unitPermitFee is required")
	private BigDecimal unitPermitFee;

	private LaborCategory laborCategory;
}
