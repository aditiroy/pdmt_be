package com.lowes.permits.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class BulkUndoActivitiesRequest {

	@NotEmpty
	private List<String> activityIds;
}
