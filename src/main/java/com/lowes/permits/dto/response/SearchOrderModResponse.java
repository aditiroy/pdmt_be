package com.lowes.permits.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lowes.permits.model.Pagination;
import com.lowes.permits.model.Sort;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchOrderModResponse {
	private List<OrderModResponse> data;
	private Pagination pagination;
	private List<Sort> sort;
}
