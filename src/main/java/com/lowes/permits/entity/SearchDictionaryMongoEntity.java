package com.lowes.permits.entity;

import java.time.Instant;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.lowes.permits.model.Item;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "search_dictionary")
public class SearchDictionaryMongoEntity {
	@Id
	private ObjectId id;

	private List<String> data;

	private List<Item> mapData;

	private String type;

	private Instant lastUpdatedAt;
}
