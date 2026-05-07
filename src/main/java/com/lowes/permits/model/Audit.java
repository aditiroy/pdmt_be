package com.lowes.permits.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Audit {
	private Long createdAt;
	private Long lastModifiedAt;
	private String createdByApplicationName;
	private String lastModifiedByApplicationName;
	private Long createdTimestamp;
	private String lastModifiedByTraceId;
	private String createdByName;
	private String createdById;
	private String createdByUserGroup;
	private String createdByUserRole;
	private String lastModifiedById;
	private String lastModifiedByUserGroup;
	private String lastModifiedByUserRole;
	private Long lastModifiedByTimestamp;
	private String lastModifiedByName;
	private String createdByEmailId;
	private String createdByJobCode;
	private String lastModifiedByEmailId;
	private String lastModifiedByJobCode;
}
