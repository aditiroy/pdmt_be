package com.lowes.permits.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EditActivityFeeRequest {
	@NotNull(message = "unitPermitFee is required")
	private BigDecimal unitPermitFee;

	private Integer estPermitObtainDays;
}
