package com.lowes.permits.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
	private String userRole;
	private String id;
	private String email;
	private String jobCode;
	private String storeNumber;
	private String providerId;
	private IDTYPE type;

	public enum IDTYPE {
		GUEST_EMAIL,
		GUEST_MOBILE,
		EC_ID,
		EMAIL;

		IDTYPE() {}
	}
}
