package com.lowes.permits.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
@Table(name = "permit_master", schema = "permitmain")
public class PermitPostgresEntity {
	@Id
	@Column("labor_category_code")
	private int laborCategoryCode;

	@Column("labor_category_description")
	private String laborCategoryDescription;

	@Column("zipcode")
	private String zipcode;

	@Column("city")
	private String city;

	@Column("state")
	private String state;

	@Column("labor_item")
	private String laborItem;

	@Column("labor_item_description")
	private String laborItemDescription;

	@Column("unit_permit_fee")
	private BigDecimal unitPermitFee;

	@Column("omni_item_id")
	private String omniItemId;

	@Column("county")
	private String county;

	@Column("provider")
	private String provider;

	@Column("vbu_number")
	private String vbuNumber;

	@Column("created_by")
	private String createdBy;

	@Column("updated_by")
	private String updatedBy;

	@Column("created_timestamp")
	private LocalDateTime createdTimestamp;

	@Column("updated_timestamp")
	private LocalDateTime updatedTimestamp;

	@Column("municipality")
	private String municipality;

	@Column("old_price")
	private String oldPrice;

	@Column("est_permit_obtain_days")
	private Integer estPermitObtainDays;
}
