package com.lowes.permits.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "order_mods", schema = "permitmain")
public class OrderModPostgresEntity {

	@Id
	@Column("id")
	private UUID id;

	@Column("labor_category_code")
	private Integer laborCategoryCode;

	@Column("labor_category_description")
	private String laborCategoryDescription;

	@Column("labor_item")
	private Integer laborItem;

	@Column("labor_item_description")
	private String laborItemDescription;

	@Column("unit_permit_fee")
	private BigDecimal unitPermitFee;

	@Column("omni_item_id")
	private String omniItemId;

	@Column("street_address")
	private String streetAddress;

	@Column("city")
	private String city;

	@Column("state")
	private String state;

	@Column("zipcode")
	private String zipcode;

	@Column("county")
	private String county;

	@Column("municipality")
	private String municipality;

	@Column("matched_address")
	private String matchedAddress;

	@Column("provider")
	private String provider;

	@Column("compliance_status")
	private String complianceStatus;

	@Column("vbu_number")
	private Integer vbuNumber;

	@Column("created_timestamp")
	private LocalDateTime createdTimestamp;

	@Column("created_by")
	private String createdBy;

	@Column("updated_by")
	private String updatedBy;

	@Column("last_updated_timestamp")
	private LocalDateTime lastUpdatedTimestamp;

	@Column("permit_insert_type")
	private String permitInsertType;

	@Column("old_permit_fee")
	private BigDecimal oldPermitFee;

	@Column("job_id")
	private String jobId;

	@Column("order_number")
	private String orderNumber;
}
