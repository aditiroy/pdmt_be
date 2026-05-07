package com.lowes.permits.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserToken {
	private User user;
	private String firstName;
	private String lastName;
	private String userGroup;
	private long timeStamp;
}
