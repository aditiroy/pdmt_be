package com.lowes.permits.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.lowes.permits.entity.PermitExportMetadataEntity;
import com.lowes.permits.exception.PermitExportNotFoundException;
import com.lowes.permits.repository.PermitMongoRepository;
import com.lowes.permits.repository.PermitPostgresRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

@ExtendWith(MockitoExtension.class)
class PermitExportServiceTest {

	private static final String TEST_BUCKET_NAME = "test-bucket";
	private static final String TEST_UPLOAD_ID = "test-upload-id-12345";
	private static final String TEST_ETAG = "test-etag-67890";

	@Mock
	private S3AsyncClient s3AsyncClient;

	@Mock
	private PermitPostgresRepository permitPostgresRepository;

	@Mock
	private PermitMongoRepository permitMongoRepository;

	@Mock
	private com.lowes.permits.model.CredentialsDto credentialsDto;

	private PermitExportService permitExportService;

	@BeforeEach
	void setUp() {
		permitExportService =
				new PermitExportService(s3AsyncClient, permitPostgresRepository, permitMongoRepository, credentialsDto);
		ReflectionTestUtils.setField(permitExportService, "bucketName", TEST_BUCKET_NAME);
		ReflectionTestUtils.setField(permitExportService, "awsRegion", "us-east-1");
		ReflectionTestUtils.setField(permitExportService, "documentUrl", "https://s3.amazonaws.com");
		ReflectionTestUtils.setField(permitExportService, "awsAccessKey", "test-access-key");
	}

	@Test
	void testExportPermitsToCsvAndUpload_Success() {
		String sql = "SELECT labor_category_code, labor_category_description, zipcode, city, state, "
				+ "labor_item, labor_item_description, unit_permit_fee, omni_item_id, county, "
				+ "provider, vbu_number, created_by, updated_by, created_timestamp, updated_timestamp, municipality, "
				+ "old_price, est_permit_obtain_days " + "FROM permitmain.permit_master ";

		byte[] chunk1 = createTestCsvData(1500);
		byte[] chunk2 = createTestCsvData(1500);
		byte[] chunk3 = createTestCsvData(500);

		CreateMultipartUploadResponse createResponse =
				CreateMultipartUploadResponse.builder().uploadId(TEST_UPLOAD_ID).build();

		UploadPartResponse uploadPartResponse1 =
				UploadPartResponse.builder().eTag("etag-part-1").build();

		UploadPartResponse uploadPartResponse2 =
				UploadPartResponse.builder().eTag("etag-part-2").build();

		UploadPartResponse uploadPartResponse3 =
				UploadPartResponse.builder().eTag("etag-part-3").build();

		CompleteMultipartUploadResponse completeResponse =
				CompleteMultipartUploadResponse.builder().eTag(TEST_ETAG).build();

		when(s3AsyncClient.createMultipartUpload(any(CreateMultipartUploadRequest.class)))
				.thenReturn(CompletableFuture.completedFuture(createResponse));

		when(permitPostgresRepository.exportPermits(sql)).thenReturn(Flux.just(chunk1, chunk2, chunk3));

		when(s3AsyncClient.uploadPart(any(UploadPartRequest.class), any(AsyncRequestBody.class)))
				.thenReturn(CompletableFuture.completedFuture(uploadPartResponse1))
				.thenReturn(CompletableFuture.completedFuture(uploadPartResponse2))
				.thenReturn(CompletableFuture.completedFuture(uploadPartResponse3));

		when(s3AsyncClient.completeMultipartUpload(any(CompleteMultipartUploadRequest.class)))
				.thenReturn(CompletableFuture.completedFuture(completeResponse));

		when(permitMongoRepository.saveExportMetadata(any(PermitExportMetadataEntity.class)))
				.thenReturn(Mono.just(
						PermitExportMetadataEntity.builder().id("metadata-id").build()));

		StepVerifier.create(permitExportService.exportPermitsToCsvAndUpload())
				.assertNext(key -> {
					assertNotNull(key);
					assertTrue(key.contains("permits/"));
					assertTrue(key.contains(".csv"));
				})
				.verifyComplete();

		verify(s3AsyncClient, times(1)).createMultipartUpload(any(CreateMultipartUploadRequest.class));
		verify(s3AsyncClient, times(3)).uploadPart(any(UploadPartRequest.class), any(AsyncRequestBody.class));
		verify(s3AsyncClient, times(1)).completeMultipartUpload(any(CompleteMultipartUploadRequest.class));
		verify(permitMongoRepository, times(1)).saveExportMetadata(any(PermitExportMetadataEntity.class));
	}

	@Test
	void testExportPermitsToCsvAndUpload_SinglePart() {
		String sql = "SELECT labor_category_code, labor_category_description, zipcode, city, state, "
				+ "labor_item, labor_item_description, unit_permit_fee, omni_item_id, county, "
				+ "provider, vbu_number, created_by, updated_by, created_timestamp, updated_timestamp, municipality, "
				+ "old_price, est_permit_obtain_days " + "FROM permitmain.permit_master ";

		byte[] smallChunk = createTestCsvData(500);

		CreateMultipartUploadResponse createResponse =
				CreateMultipartUploadResponse.builder().uploadId(TEST_UPLOAD_ID).build();

		UploadPartResponse uploadPartResponse =
				UploadPartResponse.builder().eTag("etag-part-1").build();

		CompleteMultipartUploadResponse completeResponse =
				CompleteMultipartUploadResponse.builder().eTag(TEST_ETAG).build();

		when(s3AsyncClient.createMultipartUpload(any(CreateMultipartUploadRequest.class)))
				.thenReturn(CompletableFuture.completedFuture(createResponse));

		when(permitPostgresRepository.exportPermits(sql)).thenReturn(Flux.just(smallChunk));

		when(s3AsyncClient.uploadPart(any(UploadPartRequest.class), any(AsyncRequestBody.class)))
				.thenReturn(CompletableFuture.completedFuture(uploadPartResponse));

		when(s3AsyncClient.completeMultipartUpload(any(CompleteMultipartUploadRequest.class)))
				.thenReturn(CompletableFuture.completedFuture(completeResponse));

		when(permitMongoRepository.saveExportMetadata(any(PermitExportMetadataEntity.class)))
				.thenReturn(Mono.just(
						PermitExportMetadataEntity.builder().id("metadata-id").build()));

		StepVerifier.create(permitExportService.exportPermitsToCsvAndUpload())
				.assertNext(key -> assertNotNull(key))
				.verifyComplete();

		verify(s3AsyncClient, times(1)).uploadPart(any(UploadPartRequest.class), any(AsyncRequestBody.class));
	}

	@Test
	void testExportPermitsToCsvAndUpload_EmptyData() {
		String sql = "SELECT labor_category_code, labor_category_description, zipcode, city, state, "
				+ "labor_item, labor_item_description, unit_permit_fee, omni_item_id, county, "
				+ "provider, vbu_number, created_by, updated_by, created_timestamp, updated_timestamp, municipality, "
				+ "old_price, est_permit_obtain_days " + "FROM permitmain.permit_master ";

		CreateMultipartUploadResponse createResponse =
				CreateMultipartUploadResponse.builder().uploadId(TEST_UPLOAD_ID).build();

		UploadPartResponse uploadPartResponse =
				UploadPartResponse.builder().eTag("etag-part-1").build();

		CompleteMultipartUploadResponse completeResponse =
				CompleteMultipartUploadResponse.builder().eTag(TEST_ETAG).build();

		when(s3AsyncClient.createMultipartUpload(any(CreateMultipartUploadRequest.class)))
				.thenReturn(CompletableFuture.completedFuture(createResponse));

		when(permitPostgresRepository.exportPermits(sql)).thenReturn(Flux.empty());

		when(s3AsyncClient.uploadPart(any(UploadPartRequest.class), any(AsyncRequestBody.class)))
				.thenReturn(CompletableFuture.completedFuture(uploadPartResponse));

		when(s3AsyncClient.completeMultipartUpload(any(CompleteMultipartUploadRequest.class)))
				.thenReturn(CompletableFuture.completedFuture(completeResponse));

		when(permitMongoRepository.saveExportMetadata(any(PermitExportMetadataEntity.class)))
				.thenReturn(Mono.just(
						PermitExportMetadataEntity.builder().id("metadata-id").build()));

		StepVerifier.create(permitExportService.exportPermitsToCsvAndUpload())
				.assertNext(key -> assertNotNull(key))
				.verifyComplete();

		verify(s3AsyncClient, times(1)).uploadPart(any(UploadPartRequest.class), any(AsyncRequestBody.class));
	}

	@Test
	void testExportPermitsToCsvAndUpload_MetadataSaveFailure() {
		String sql = "SELECT labor_category_code, labor_category_description, zipcode, city, state, "
				+ "labor_item, labor_item_description, unit_permit_fee, omni_item_id, county, "
				+ "provider, vbu_number, created_by, updated_by, created_timestamp, updated_timestamp, municipality, "
				+ "old_price, est_permit_obtain_days " + "FROM permitmain.permit_master ";

		byte[] smallChunk = createTestCsvData(500);

		CreateMultipartUploadResponse createResponse =
				CreateMultipartUploadResponse.builder().uploadId(TEST_UPLOAD_ID).build();

		UploadPartResponse uploadPartResponse =
				UploadPartResponse.builder().eTag("etag-part-1").build();

		CompleteMultipartUploadResponse completeResponse =
				CompleteMultipartUploadResponse.builder().eTag(TEST_ETAG).build();

		when(s3AsyncClient.createMultipartUpload(any(CreateMultipartUploadRequest.class)))
				.thenReturn(CompletableFuture.completedFuture(createResponse));

		when(permitPostgresRepository.exportPermits(sql)).thenReturn(Flux.just(smallChunk));

		when(s3AsyncClient.uploadPart(any(UploadPartRequest.class), any(AsyncRequestBody.class)))
				.thenReturn(CompletableFuture.completedFuture(uploadPartResponse));

		when(s3AsyncClient.completeMultipartUpload(any(CompleteMultipartUploadRequest.class)))
				.thenReturn(CompletableFuture.completedFuture(completeResponse));

		when(permitMongoRepository.saveExportMetadata(any(PermitExportMetadataEntity.class)))
				.thenReturn(Mono.error(new RuntimeException("MongoDB save failed")));

		StepVerifier.create(permitExportService.exportPermitsToCsvAndUpload())
				.expectError(RuntimeException.class)
				.verify();

		verify(s3AsyncClient, times(1)).completeMultipartUpload(any(CompleteMultipartUploadRequest.class));
	}

	@Test
	void testExportPermitsToCsvAndUpload_VerifyS3RequestParameters() {
		String sql = "SELECT labor_category_code, labor_category_description, zipcode, city, state, "
				+ "labor_item, labor_item_description, unit_permit_fee, omni_item_id, county, "
				+ "provider, vbu_number, created_by, updated_by, created_timestamp, updated_timestamp, municipality, "
				+ "old_price, est_permit_obtain_days " + "FROM permitmain.permit_master ";

		byte[] chunk = createTestCsvData(500);

		CreateMultipartUploadResponse createResponse =
				CreateMultipartUploadResponse.builder().uploadId(TEST_UPLOAD_ID).build();

		UploadPartResponse uploadPartResponse =
				UploadPartResponse.builder().eTag("etag-part-1").build();

		CompleteMultipartUploadResponse completeResponse =
				CompleteMultipartUploadResponse.builder().eTag(TEST_ETAG).build();

		ArgumentCaptor<CreateMultipartUploadRequest> createRequestCaptor =
				ArgumentCaptor.forClass(CreateMultipartUploadRequest.class);
		ArgumentCaptor<UploadPartRequest> uploadPartRequestCaptor = ArgumentCaptor.forClass(UploadPartRequest.class);
		ArgumentCaptor<CompleteMultipartUploadRequest> completeRequestCaptor =
				ArgumentCaptor.forClass(CompleteMultipartUploadRequest.class);

		when(s3AsyncClient.createMultipartUpload(createRequestCaptor.capture()))
				.thenReturn(CompletableFuture.completedFuture(createResponse));

		when(permitPostgresRepository.exportPermits(sql)).thenReturn(Flux.just(chunk));

		when(s3AsyncClient.uploadPart(uploadPartRequestCaptor.capture(), any(AsyncRequestBody.class)))
				.thenReturn(CompletableFuture.completedFuture(uploadPartResponse));

		when(s3AsyncClient.completeMultipartUpload(completeRequestCaptor.capture()))
				.thenReturn(CompletableFuture.completedFuture(completeResponse));

		when(permitMongoRepository.saveExportMetadata(any(PermitExportMetadataEntity.class)))
				.thenReturn(Mono.just(
						PermitExportMetadataEntity.builder().id("metadata-id").build()));

		StepVerifier.create(permitExportService.exportPermitsToCsvAndUpload())
				.assertNext(key -> assertNotNull(key))
				.verifyComplete();

		CreateMultipartUploadRequest createRequest = createRequestCaptor.getValue();
		assertEquals(TEST_BUCKET_NAME, createRequest.bucket());
		assertEquals("text/csv", createRequest.contentType());
		assertTrue(createRequest.key().contains("permits/"));

		UploadPartRequest uploadPartRequest = uploadPartRequestCaptor.getValue();
		assertEquals(TEST_BUCKET_NAME, uploadPartRequest.bucket());
		assertEquals(TEST_UPLOAD_ID, uploadPartRequest.uploadId());
		assertEquals(1, uploadPartRequest.partNumber());

		CompleteMultipartUploadRequest completeRequest = completeRequestCaptor.getValue();
		assertEquals(TEST_BUCKET_NAME, completeRequest.bucket());
		assertEquals(TEST_UPLOAD_ID, completeRequest.uploadId());
		assertNotNull(completeRequest.multipartUpload());
	}

	@Test
	void testBuildS3Key() {
		String fileName = "test-file.csv";
		String key = ReflectionTestUtils.invokeMethod(permitExportService, "buildS3Key", fileName);

		assertNotNull(key);
		assertEquals("permits/test-file.csv", key);
	}

	@Test
	void testCombineByteArrays() {
		byte[] array1 = "Hello ".getBytes(StandardCharsets.UTF_8);
		byte[] array2 = "World".getBytes(StandardCharsets.UTF_8);
		byte[] array3 = "!".getBytes(StandardCharsets.UTF_8);

		byte[] result = ReflectionTestUtils.invokeMethod(
				permitExportService, "combineByteArrays", java.util.List.of(array1, array2, array3));

		assertNotNull(result);
		assertEquals("Hello World!", new String(result, StandardCharsets.UTF_8));
	}

	@Test
	void testCombineByteArrays_EmptyList() {
		byte[] result = ReflectionTestUtils.invokeMethod(permitExportService, "combineByteArrays", java.util.List.of());

		assertNotNull(result);
		assertEquals(0, result.length);
	}

	@Test
	void testCombineByteArrays_SingleArray() {
		byte[] array = "SingleArray".getBytes(StandardCharsets.UTF_8);

		byte[] result =
				ReflectionTestUtils.invokeMethod(permitExportService, "combineByteArrays", java.util.List.of(array));

		assertNotNull(result);
		assertEquals("SingleArray", new String(result, StandardCharsets.UTF_8));
	}

	@Test
	void testGeneratePreSignedDownloadUrl_Success() {
		PermitExportMetadataEntity metadata = PermitExportMetadataEntity.builder()
				.fileName("permits-export.csv")
				.folderPath("permits/permits-export.csv")
				.build();

		when(permitMongoRepository.findLatestFileByTypeAndStatus("ALL_PERMITS")).thenReturn(Mono.just(metadata));
		when(credentialsDto.getDocumentAccessSecret()).thenReturn("test-secret");

		StepVerifier.create(permitExportService.generatePreSignedDownloadUrl())
				.assertNext(response -> {
					assertNotNull(response);
					assertTrue(response.containsKey("type"));
					assertTrue(response.containsKey("preSignedUrl"));
					assertEquals("ALL_PERMITS", response.get("type"));
					assertNotNull(response.get("preSignedUrl"));
				})
				.verifyComplete();

		verify(permitMongoRepository, times(1)).findLatestFileByTypeAndStatus("ALL_PERMITS");
	}

	@Test
	void testGeneratePreSignedDownloadUrl_NoMetadataFound() {
		when(permitMongoRepository.findLatestFileByTypeAndStatus("ALL_PERMITS")).thenReturn(Mono.empty());

		StepVerifier.create(permitExportService.generatePreSignedDownloadUrl())
				.expectErrorMatches(throwable -> throwable instanceof PermitExportNotFoundException
						&& throwable
								.getMessage()
								.equals("No permit report data found. Please generate the report first."))
				.verify();

		verify(permitMongoRepository, times(1)).findLatestFileByTypeAndStatus("ALL_PERMITS");
	}

	private byte[] createTestCsvData(int sizeInKB) {
		StringBuilder csvData = new StringBuilder();
		int rowSize = 100;
		int numRows = (sizeInKB * 1024) / rowSize;

		for (int i = 0; i < numRows; i++) {
			csvData.append(
					"123,Labor Category,90210,Los Angeles,CA,456,Labor Item,100.50,OMNI123,LA County,Provider,789,user1,user2,2024-01-01,2024-01-02,City,50.25,5\n");
		}

		return csvData.toString().getBytes(StandardCharsets.UTF_8);
	}
}
