package com.lowes.permits.dto.response;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(collection = "labor_category")
public class LaborCategoryResponse {
	@JsonIgnore
	@Id
	private ObjectId id;

	@JsonProperty("id")
	private String laborCategoryId;

	private String name;
	private String type;
	private String group;
	private String installationType;
	private String status;
	private Boolean enableForDetailScheduling;
	private Boolean epaLaborCategory;
	private Integer averageJobSize;
	private String insuranceTierId;
	private String insuranceTierName;
	private String jsiPriority;
	private String createdOn;
	private String modifiedOn;
	private String manualCall;

	// Custom setter for JSON "id" field mapping to laborCategoryId
	public void setId(String id) {
		this.laborCategoryId = id;
	}

	// Provide getter for MongoDB ObjectId
	public ObjectId getMongoId() {
		return this.id;
	}

	// Provide setter for MongoDB ObjectId
	public void setMongoId(ObjectId id) {
		this.id = id;
	}
}
