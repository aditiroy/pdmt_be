package com.lowes.permits.constants;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class ApplicationConstants {
	public static final String X_USER_TOKEN = "X-USER-TOKEN";
	public static final String X_B3_TRACE_ID = "X-B3-TRACE-ID";
	public static final String X_CALLER_APP = "X-CALLER-APP";

	public static final String STATUS = "status";
	public static final String NEW = "NEW";
	public static final String UPDATE = "UPDATE";
	public static final String DELETE = "DELETE";
	public static final String RETRY_STATE = "RETRY_STATE";
	public static final String RETRY_COUNT = "retryCount";
	public static final String AUDIT = "audit";
	public static final String UNIT_PERMIT_FEE = "unitPermitFee";
	public static final String OPERATION_TYPE = "operationType";
	public static final String ERROR_MESSAGE = "errorMessage";
	public static final String PERMIT_DB_ID = "permitDbId";
	public static final String RETRY = "RETRY";
	public static final String ORDER_MODS_TO_PERMIT = "ORDER_MODS_TO_PERMIT";
	public static final String LABOR_CATEGORY = "LABOR_CATEGORY";
	public static final String ALL_PERMITS_UPLOAD = "ALL_PERMITS_UPLOAD";
	public static final String PERMIT_CSV_FILENAME = "all_permits.csv";
	public static final String CONTENT_TYPE_CSV = "text/csv";
	public static final String SUCCESS = "SUCCESS";
	public static final String AVS_SUCCESS = "AVS_SUCCESS";
	public static final String APPROVED = "APPROVED";
	public static final String REJECTED = "REJECTED";
	public static final String ALL_PERMITS = "ALL_PERMITS";
	public static final String LAST_MODIFIED_AT = "audit.lastModifiedAt";
	public static final String PROCESSED = "PROCESSED";
	public static final String TYPE = "type";
	public static final String PRE_SIGNED_URL = "preSignedUrl";

	public static final String ROOT = "root";
	public static final String ROOT_EXTENSION = ".pem";
	public static final String MASKING_PATTERN_REGEX_PREFIX = "(?<=";
	public static final String MASKING_PATTERN_REGEX_SUFFIX =
			"\\\\?\"?[=:])(?: ?\\\\?\"?)(.*?)(?:]?)(?:]?\\)?\\\\?\")?,";
	public static final int IGNORE_START_FOR_EMAIL = 1;
	public static final int IGNORE_END_FOR_EMAIL = 1;
	public static final int IGNORE_START_FOR_PHONENUMBER = 0;
	public static final int IGNORE_END_FOR_PHONENUMBER = 4;
	public static final int IGNORE_START_FOR_CREDITCARD = 0;
	public static final int IGNORE_END_FOR_CREDITCARD = 4;

	public static final String TRACE_ID_HEADER = "x-b3-traceid";
	public static final String SPAN_ID_HEADER = "x-b3-spanid";
	public static final String MDC_TRACE_ID_KEY = "traceId";
	public static final String UNKNOWN_APPLICATION = "UNKNOWN-APPLICATION";
	public static final String SYSTEM = "SYSTEM";

	public static final String DUMMY_VALUE = "DUMMY_VALUE";
}
