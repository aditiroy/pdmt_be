package com.lowes.permits.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Pagination {
	private Integer page;
	private Integer pageSize;
	private Long totalCount;
	private Boolean hasNextPage;
}
