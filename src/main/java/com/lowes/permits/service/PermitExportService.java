package com.lowes.permits.service;

import static com.lowes.permits.constants.ApplicationConstants.ALL_PERMITS;
import static com.lowes.permits.constants.ApplicationConstants.CONTENT_TYPE_CSV;
import static com.lowes.permits.constants.ApplicationConstants.PERMIT_CSV_FILENAME;
import static com.lowes.permits.constants.ApplicationConstants.PRE_SIGNED_URL;
import static com.lowes.permits.constants.ApplicationConstants.SUCCESS;
import static com.lowes.permits.constants.ApplicationConstants.TYPE;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.lowes.permits.entity.PermitExportMetadataEntity;
import com.lowes.permits.exception.PermitExportNotFoundException;
import com.lowes.permits.repository.PermitMongoRepository;
import com.lowes.permits.repository.PermitPostgresRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Service
@Slf4j
@RequiredArgsConstructor
public class PermitExportService {

	private static final String FOLDER_SEPARATOR = "/";
	private static final int MULTIPART_MIN_SIZE = 1024 * 1024; // 1 MB minimum per part
	private static final String PERMIT_FOLDER_PATH = "permits";

	private final S3AsyncClient s3AsyncClient;
	private final PermitPostgresRepository permitPostgresRepository;
	private final PermitMongoRepository permitMongoRepository;
	private final com.lowes.permits.model.CredentialsDto credentialsDto;

	@Value("${document.bucket.name}")
	private String bucketName;

	@Value("${aws.region:us-east-1}")
	private String awsRegion;

	@Value("${document.url}")
	private String documentUrl;

	@Value("${document.access.name}")
	private String awsAccessKey;

	private String buildS3Key(String fileName) {
		return PERMIT_FOLDER_PATH + FOLDER_SEPARATOR + fileName;
	}

	public Mono<String> exportPermitsToCsvAndUpload() {
		log.info("===== STARTING PERMIT CSV EXPORT TO S3 BUCKET=====");

		String sql = "SELECT labor_category_code, labor_category_description, zipcode, city, state, "
				+ "labor_item, labor_item_description, unit_permit_fee, omni_item_id, county, "
				+ "provider, vbu_number, created_by, updated_by, created_timestamp, updated_timestamp, municipality, "
				+ "old_price, est_permit_obtain_days " + "FROM permitmain.permit_master ";

		String fileName = PERMIT_CSV_FILENAME;
		String key = buildS3Key(fileName);

		log.info("Uploading CSV to bucket: {}, key: {}", bucketName, key);

		String header = "Labor Category Code,Labor Category Description,Zipcode,City,State,"
				+ "Labor Item,Labor Item Description,Unit Permit Fee,Omni Item ID,County,"
				+ "Provider,VBU Number,Created By,Updated By,Created Timestamp,Updated Timestamp,Municipality,"
				+ "Old Price,Est Permit Obtain Days\n";

		// Stream directly to S3 using multipart upload
		return initiateMultipartUpload(key)
				.flatMap(uploadId -> {
					log.info("Multipart upload initiated with ID: {}", uploadId);
					log.info(
							"Buffer threshold: {} bytes ({}MB)",
							MULTIPART_MIN_SIZE,
							MULTIPART_MIN_SIZE / (1024 * 1024));

					List<CompletedPart> completedParts = new ArrayList<>();
					AtomicInteger partNumber = new AtomicInteger(1);
					List<byte[]> partBuffer = new ArrayList<>();
					AtomicInteger bufferSize = new AtomicInteger(0);
					AtomicInteger fileSize = new AtomicInteger(0);

					// Add header to first part
					byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);
					partBuffer.add(headerBytes);
					bufferSize.addAndGet(headerBytes.length);
					log.info("CSV header added to buffer ({} bytes)", headerBytes.length);

					return permitPostgresRepository
							.exportPermits(sql)
							.doOnSubscribe(s -> log.info(" Starting to stream data from PostgreSQL..."))
							.concatMap(data -> {
								int chunkSize = data.length;
								fileSize.addAndGet(chunkSize);

								log.info(
										"PostgreSQL data: received {} bytes | Total received: {} bytes ({} MB) | Current buffer: {} bytes",
										chunkSize,
										fileSize.get(),
										fileSize.get() / (1024 * 1024),
										bufferSize.get());

								partBuffer.add(data);
								bufferSize.addAndGet(data.length);

								if (bufferSize.get() >= MULTIPART_MIN_SIZE) {
									int currentPartNumber = partNumber.getAndIncrement();
									byte[] partData = combineByteArrays(new ArrayList<>(partBuffer));
									int partSize = partData.length;
									partBuffer.clear();
									bufferSize.set(0);

									log.info(
											"S3 UPLOAD: Part #{} | Size: {} bytes ({} MB) | Total data processed: {} MB",
											currentPartNumber,
											partSize,
											partSize / (1024 * 1024),
											fileSize.get() / (1024 * 1024));

									return uploadPart(key, uploadId, currentPartNumber, partData)
											.doOnSuccess(part -> {
												completedParts.add(part);
												log.info(
														"✓ S3 Part #{} uploaded successfully (ETag: {})",
														currentPartNumber,
														part.eTag());
											})
											.doOnError(
													e -> log.error("✗ S3 Part #{} upload FAILED", currentPartNumber, e))
											.then();
								}
								return Mono.empty();
							})
							.doOnComplete(() -> log.info(
									"PostgreSQL streaming completed. Total bytes: {} ({} MB)",
									fileSize.get(),
									fileSize.get() / (1024 * 1024)))
							.then(Mono.defer(() -> {
								// Upload remaining data as final part
								if (!partBuffer.isEmpty()) {
									int finalPartNumber = partNumber.get();
									byte[] finalPartData = combineByteArrays(partBuffer);
									log.info(
											"S3 UPLOAD: Final part #{} | Size: {} bytes ({} MB) | Remaining buffer data",
											finalPartNumber,
											finalPartData.length,
											finalPartData.length / (1024 * 1024));

									return uploadPart(key, uploadId, finalPartNumber, finalPartData)
											.doOnSuccess(part -> {
												completedParts.add(part);
												log.info(
														"S3 Final part #{} uploaded successfully (ETag: {})",
														finalPartNumber,
														part.eTag());
											})
											.doOnError(e -> log.error("S3 Final part upload FAILED", e))
											.then();
								}
								log.info("No remaining buffer data to upload");
								return Mono.empty();
							}))
							.then(Mono.defer(() -> {
								log.info("Completing multipart upload: {} parts total", completedParts.size());
								return completeMultipartUpload(key, uploadId, completedParts);
							}))
							.flatMap(response -> {
								log.info("===== CSV EXPORT COMPLETED SUCCESSFULLY =====");
								log.info("S3 Location: s3://{}/{}", bucketName, key);
								log.info("Total S3 parts uploaded: {}", completedParts.size());
								log.info(
										"Total bytes uploaded: {} ({} MB)",
										fileSize.get(),
										fileSize.get() / (1024 * 1024));

								return saveMetadata(
												fileName,
												key,
												bucketName,
												(long) fileSize.get(),
												response,
												SUCCESS,
												null)
										.thenReturn(key);
							});
				})
				.onErrorResume(e -> {
					log.error("===== CSV EXPORT FAILED =====", e);
					// Save metadata with FAILED status
					return saveMetadata(fileName, key, bucketName, 0L, null, "FAILED", e.getMessage())
							.then(Mono.error(e));
				});
	}

	private Mono<String> initiateMultipartUpload(String key) {
		CreateMultipartUploadRequest request = CreateMultipartUploadRequest.builder()
				.bucket(bucketName)
				.key(key)
				.contentType(CONTENT_TYPE_CSV)
				.build();

		return Mono.fromFuture(s3AsyncClient.createMultipartUpload(request))
				.map(CreateMultipartUploadResponse::uploadId);
	}

	private Mono<CompletedPart> uploadPart(String key, String uploadId, int partNumber, byte[] data) {
		UploadPartRequest request = UploadPartRequest.builder()
				.bucket(bucketName)
				.key(key)
				.uploadId(uploadId)
				.partNumber(partNumber)
				.contentLength((long) data.length)
				.build();

		return Mono.fromFuture(s3AsyncClient.uploadPart(request, AsyncRequestBody.fromBytes(data)))
				.map(response -> CompletedPart.builder()
						.partNumber(partNumber)
						.eTag(response.eTag())
						.build());
	}

	private Mono<CompleteMultipartUploadResponse> completeMultipartUpload(
			String key, String uploadId, List<CompletedPart> parts) {
		CompletedMultipartUpload completedUpload =
				CompletedMultipartUpload.builder().parts(parts).build();

		CompleteMultipartUploadRequest request = CompleteMultipartUploadRequest.builder()
				.bucket(bucketName)
				.key(key)
				.uploadId(uploadId)
				.multipartUpload(completedUpload)
				.build();

		return Mono.fromFuture(s3AsyncClient.completeMultipartUpload(request));
	}

	private byte[] combineByteArrays(List<byte[]> arrays) {
		int totalSize = arrays.stream().mapToInt(array -> array.length).sum();
		ByteBuffer buffer = ByteBuffer.allocate(totalSize);
		for (byte[] array : arrays) {
			buffer.put(array);
		}
		return buffer.array();
	}

	public Mono<Map<String, Object>> generatePreSignedDownloadUrl() {
		return permitMongoRepository
				.findLatestFileByTypeAndStatus(ALL_PERMITS)
				.switchIfEmpty(Mono.error(new PermitExportNotFoundException(
						"No permit report data found. Please generate the report first.")))
				.flatMap(metadata -> {
					String key = metadata.getFolderPath();
					log.info("Generating preSigned URL for bucket: {}, key: {}", bucketName, key);

					return Mono.fromCallable(() -> {
								try (S3Presigner presigner = S3Presigner.builder()
										.region(Region.of(awsRegion))
										.endpointOverride(new URI(documentUrl))
										.credentialsProvider(
												StaticCredentialsProvider.create(AwsBasicCredentials.create(
														awsAccessKey, credentialsDto.getDocumentAccessSecret())))
										.serviceConfiguration(S3Configuration.builder()
												.pathStyleAccessEnabled(true)
												.build())
										.build()) {
									GetObjectRequest getObjectRequest = GetObjectRequest.builder()
											.bucket(bucketName)
											.key(key)
											.build();

									GetObjectPresignRequest preSignRequest = GetObjectPresignRequest.builder()
											.signatureDuration(Duration.ofMinutes(15))
											.getObjectRequest(getObjectRequest)
											.build();

									PresignedGetObjectRequest preSignedRequest =
											presigner.presignGetObject(preSignRequest);

									log.info("Successfully generated presigned URL, expires in 15 minutes");

									Map<String, Object> response = new HashMap<>();
									response.put(TYPE, ALL_PERMITS);
									response.put(
											PRE_SIGNED_URL,
											preSignedRequest.url().toString());

									return response;
								}
							})
							.onErrorResume(NoSuchKeyException.class, e -> {
								log.error("File not found in S3: bucket={}, key={}", bucketName, key, e);
								return Mono.error(new PermitExportNotFoundException(
										"Permit file not found in storage. The file may have been deleted or moved."));
							})
							.onErrorResume(throwable -> {
								if (!(throwable instanceof PermitExportNotFoundException)) {
									log.error(
											"Failed to generate presigned URL: bucket={}, key={}",
											bucketName,
											key,
											throwable);
									return Mono.error(new RuntimeException(
											"Failed to generate presigned download URL", throwable));
								}
								return Mono.error(throwable);
							});
				});
	}

	private Mono<PermitExportMetadataEntity> saveMetadata(
			String fileName,
			String folderPath,
			String bucketName,
			Long fileSize,
			CompleteMultipartUploadResponse s3Response,
			String uploadStatus,
			String errorMessage) {

		PermitExportMetadataEntity metadata = PermitExportMetadataEntity.builder()
				.fileName(fileName)
				.folderPath(folderPath)
				.bucketName(bucketName)
				.contentType(CONTENT_TYPE_CSV)
				.fileSize(fileSize)
				.uploadStatus(uploadStatus)
				.type(ALL_PERMITS)
				.updatedAt(Instant.now().toEpochMilli())
				.clientResponse(s3Response.toString())
				.errorMessage(errorMessage)
				.build();

		return permitMongoRepository
				.saveExportMetadata(metadata)
				.doOnSuccess(savedEntity -> log.info(
						"Export metadata saved to MongoDB with ID: {}, Status: {}",
						savedEntity.getId(),
						savedEntity.getUploadStatus()))
				.doOnError(ex -> log.error("Failed to save export metadata to MongoDB", ex));
	}
}
