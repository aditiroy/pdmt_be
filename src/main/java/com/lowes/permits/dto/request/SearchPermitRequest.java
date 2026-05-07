package com.lowes.permits.dto.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lowes.permits.model.Pagination;
import com.lowes.permits.model.PermitFilter;
import com.lowes.permits.model.Sort;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchPermitRequest {
	private PermitFilter filter;
	private List<Sort> sort;
	private Pagination pagination;
}
