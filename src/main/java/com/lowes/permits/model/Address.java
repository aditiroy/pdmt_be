package com.lowes.permits.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {

	private String country;

	@NotBlank(message = "address.state is required")
	private String state;

	private String county;

	private String municipality;

	@NotBlank(message = "address.city is required")
	private String city;

	@NotBlank(message = "address.zipCode is required")
	private String zipCode;

	private String addressLine1;
	private String addressLine2;
	private String matchedAddress;
}
