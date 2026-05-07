package com.lowes.permits.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.lowes.permits.model.Address;
import com.lowes.permits.model.Audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "order-mods")
public class OrderModMongoEntity {

	@Id
	private String id;

	private Integer categoryCode;
	private String categoryDesc;
	private String itemId;
	private String itemDesc;
	private String permitFee;
	private String omniId;
	private Address address;
	private String provider;
	private String complianceStatus;

	@Indexed
	private String status;

	@Field("vbu_number")
	private Integer vbuNumber;

	@Indexed
	private String createdTimestamp;

	private String updatedTimestamp;
	private String permitInsertType;
	private String oldPermitFee;
	private String jobId;
	private String orderNumber;
	private Integer retryCount;
	private String message;
	private String errorMessage;
	private Audit audit;
}
