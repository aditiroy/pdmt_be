package com.lowes.permits.dto.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public class ItemRequest {
	private List<OmniItemId> omniItemIds;
	private String responseGroup;
	private List<String> clients;
	private String site;
	private String clientType;

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Builder
	public static class OmniItemId {
		private String itemNumber;
		private Integer qty;
	}
}
