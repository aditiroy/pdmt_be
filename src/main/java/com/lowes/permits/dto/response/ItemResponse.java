package com.lowes.permits.dto.response;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import lombok.Data;

@Data
public class ItemResponse {
	private Map<String, ItemData> items;

	@JsonAnySetter
	public void setItem(String key, ItemData value) {
		if (items == null) {
			items = new java.util.HashMap<>();
		}
		items.put(key, value);
	}

	@Data
	public static class ItemData {
		private Product product;
		private Location location;
	}

	@Data
	public static class Product {
		private String omniItemId;
		private String barcode;
		private String description;
		private Boolean energyStar;
		private String interpackDesc;
		private String interpackCode;
		private String isPublished;
		private String isBuyable;
		private String itemNumber;
		private List<String> itemBehavior;
		private Boolean unzippedEligible;
		private Boolean installAvailInd;
		private String lastModifiedBy;
		private Boolean lowesExclusive;
		private LaborDetails laborDetails;
		private String lcomIndicator;
		private String modelId;
		private Integer orderItemMulQty;
		private Integer orderItemMinQty;
		private String pdURL;
		private Integer productTypeCode;
		private String status;
		private String sosFreightType;
		private String vendorNumber;
		private String vendorDirect;
		private MerchandisingHierarchy merchandisingHierarchy;
		private Boolean mainModel;
		private Boolean highRiskIndicator;
		private String lin;
		private ProgramType programType;
	}

	@Data
	public static class LaborDetails {
		private String laborCgyCode;
		private String laborCgyDesc;
		private String laborItemTypeCode;
		private String laborItemTypeDesc;
		private Boolean isType2;
		private String laborServiceType;
		private String laborItemType;
		private String laborDescription;
	}

	@Data
	public static class MerchandisingHierarchy {
		private String assortment;
		private String assortmentDescription;
		private String productGroup;
		private String productGroupDescription;
		private String subDivision;
		private String subDivisionDescription;
		private String division;
		private String divisionDescription;
		private String businessArea;
		private String businessAreaDescription;
	}

	@Data
	public static class ProgramType {
		private String value;
	}

	@Data
	public static class Location {
		private String zipcode;
		private String storeNumber;
		private Boolean eligibleToPurchase;
		private Boolean additionalServices;
		private Boolean sosVendorDirect;
	}
}
