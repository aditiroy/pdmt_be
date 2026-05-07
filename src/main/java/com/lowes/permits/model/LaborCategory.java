package com.lowes.permits.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LaborCategory {
	@NotNull(message = "laborCategory.code is required")
	private Integer code;

	@NotBlank(message = "laborCategory.description is required")
	private String description;
}
