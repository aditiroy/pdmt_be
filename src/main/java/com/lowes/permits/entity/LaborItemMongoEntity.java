package com.lowes.permits.entity;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "labor_items")
public class LaborItemMongoEntity {

	@Indexed
	String laborItem;

	@Id
	private ObjectId id;

	String laborItemDescription;
	String omniItemId;
	String source;
	LaborCategory laborCategory;
	private Long createdAt;
	private Long lastModifiedAt;

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class LaborCategory {
		private Integer code;
		private String description;
	}
}
