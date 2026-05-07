package com.lowes.permits.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "permit_file_metadata")
public class PermitExportMetadataEntity {
	@Id
	private String id;

	private String fileName;
	private String folderPath;
	private String bucketName;
	private String contentType;
	private Long fileSize;
	private String uploadStatus;
	private String type;
	private Long updatedAt;
	private String exportedBy;
	private String errorMessage;
	private String clientResponse;
}
