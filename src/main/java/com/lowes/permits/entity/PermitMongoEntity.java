package com.lowes.permits.entity;

import java.math.BigDecimal;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.lowes.permits.model.Address;
import com.lowes.permits.model.Audit;
import com.lowes.permits.model.LaborCategory;
import com.lowes.permits.model.OperationType;
import com.lowes.permits.model.PermitStatus;
import com.lowes.permits.model.Provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "permits")
@CompoundIndex(
		name = "unique_permitDbId_status_operationType",
		def = "{'permitDbId': 1, 'status': 1, 'operationType': 1}",
		unique = true)
public class PermitMongoEntity {
	@Id
	private String id;

	@Indexed
	private String permitDbId;

	private Integer laborItem;
	private String laborItemDescription;
	private BigDecimal unitPermitFee;
	private BigDecimal oldUnitPermitFee;
	private Integer estPermitObtainDays;
	private Integer oldEstPermitObtainDays;
	private String omniItemId;
	private LaborCategory laborCategory;
	private Address address;
	private Provider provider;
	private Audit audit;
	private PermitStatus status;
	private OperationType operationType;
	private Integer retryCount;
	private String errorMessage;
}
