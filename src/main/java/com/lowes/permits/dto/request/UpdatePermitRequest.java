package com.lowes.permits.dto.request;

import java.math.BigDecimal;

import com.lowes.permits.model.LaborCategory;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePermitRequest {

	@NotNull(message = "permitDbId is required")
	private String permitDbId;

	private LaborCategory laborCategory;

	private BigDecimal oldUnitPermitFee;

	private BigDecimal unitPermitFee;

	private Integer oldEstPermitObtainDays;

	private Integer estPermitObtainDays;

	@AssertTrue(message = "At least one of unitPermitFee or estPermitObtainDays must be provided")
	public boolean isAtLeastOneUpdateFieldPresent() {
		return unitPermitFee != null || estPermitObtainDays != null;
	}
}
