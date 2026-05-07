package com.lowes.permits.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowes.permits.exception.InvalidUserTokenException;
import com.lowes.permits.model.PermitDbKey;
import com.lowes.permits.model.User;
import com.lowes.permits.model.UserToken;

@ExtendWith(MockitoExtension.class)
class PermitPostgresEntityUtilsTest {

	private static final String SEP = "\u001E";
	private UserToken testUserToken;
	private User testUser;
	private String testUserTokenString;

	@BeforeEach
	void setUp() {
		testUser = new User();
		testUser.setId("user123");
		testUser.setUserRole("ADMIN");

		testUserToken = new UserToken();
		testUserToken.setUser(testUser);
		testUserToken.setFirstName("John");
		testUserToken.setLastName("Doe");
		testUserToken.setUserGroup("ADMINS");
		testUserToken.setTimeStamp(System.currentTimeMillis());

		// Create a valid token by encoding the user token as JSON
		testUserTokenString = createValidUserToken();
	}

	@Test
	void testGetObjectMapper() {
		// Test getObjectMapper method
		ObjectMapper mapper = PermitUtils.getObjectMapper();

		assertNotNull(mapper);
		// The same instance should be returned
		assertSame(mapper, PermitUtils.getObjectMapper());
	}

	@Test
	void testResolveTraceIdWithValidTraceId() {
		// Test resolveTraceId method with valid trace ID
		String validTraceId = "trace-123";

		String result = PermitUtils.resolveTraceId(validTraceId);

		assertEquals(validTraceId, result);
	}

	@Test
	void testResolveTraceIdWithNullTraceId() {
		// Test resolveTraceId method with null trace ID
		String result = PermitUtils.resolveTraceId(null);

		assertNotNull(result);
		assertFalse(result.isEmpty());
		// Should be a valid UUID
		assertDoesNotThrow(() -> UUID.fromString(result));
	}

	@Test
	void testResolveTraceIdWithEmptyTraceId() {
		// Test resolveTraceId method with empty trace ID
		String emptyTraceId = "";
		String result = PermitUtils.resolveTraceId(emptyTraceId);

		assertNotNull(result);
		assertFalse(result.isEmpty());
		// Should be a valid UUID
		assertDoesNotThrow(() -> UUID.fromString(result));
	}

	@Test
	void testResolveTraceIdWithBlankTraceId() {
		// Test resolveTraceId method with blank trace ID
		String blankTraceId = "   ";

		String result = PermitUtils.resolveTraceId(blankTraceId);

		assertNotNull(result);
		assertFalse(result.isEmpty());
		// Should be a valid UUID
		assertDoesNotThrow(() -> UUID.fromString(result));
	}

	@Test
	void testResolveTraceIdWithWhitespaceTraceId() {
		// Test resolveTraceId method with whitespace-only trace ID
		String whitespaceTraceId = "\t\n\r ";

		String result = PermitUtils.resolveTraceId(whitespaceTraceId);

		assertNotNull(result);
		assertFalse(result.isEmpty());
		// Should be a valid UUID
		assertDoesNotThrow(() -> UUID.fromString(result));
	}

	@Test
	void testParseUserTokenWithValidToken() {
		// Test parseUserToken method with valid token
		UserToken result = PermitUtils.parseUserToken(testUserTokenString);

		assertNotNull(result);
		assertEquals("John", result.getFirstName());
		assertEquals("Doe", result.getLastName());
		assertEquals("ADMINS", result.getUserGroup());
		assertEquals(testUser, result.getUser());
		assertTrue(result.getTimeStamp() > 0);
	}

	@Test
	void testParseUserTokenWithNullToken() {
		// Test parseUserToken method with null token
		assertThrows(InvalidUserTokenException.class, () -> {
			PermitUtils.parseUserToken(null);
		});
	}

	@Test
	void testParseUserTokenWithEmptyToken() {
		// Test parseUserToken method with empty token
		assertThrows(InvalidUserTokenException.class, () -> {
			PermitUtils.parseUserToken("");
		});
	}

	@Test
	void testParseUserTokenWithInvalidToken() {
		// Test parseUserToken method with invalid token
		String invalidToken = "invalid-base64-token";

		assertThrows(InvalidUserTokenException.class, () -> {
			PermitUtils.parseUserToken(invalidToken);
		});
	}

	@Test
	void testParseUserTokenWithMalformedJson() {
		// Test parseUserToken method with malformed JSON
		String malformedJson = Base64.getUrlEncoder().encodeToString("{\"invalid-json".getBytes());

		assertThrows(InvalidUserTokenException.class, () -> {
			PermitUtils.parseUserToken(malformedJson);
		});
	}

	@Test
	void testParseUserTokenWithNullFields() {
		// Test parseUserToken method with null fields in JSON
		String tokenWithNulls = createTokenWithNullFields();

		UserToken result = PermitUtils.parseUserToken(tokenWithNulls);

		assertNotNull(result);
		assertNull(result.getFirstName());
		assertNull(result.getLastName());
		assertNull(result.getUserGroup());
		assertNull(result.getUser());
		assertTrue(result.getTimeStamp() > 0);
	}

	@Test
	void testParseUserTokenWithEmptyFields() {
		// Test parseUserToken method with empty fields in JSON
		String tokenWithEmptyFields = createTokenWithEmptyFields();

		UserToken result = PermitUtils.parseUserToken(tokenWithEmptyFields);

		assertNotNull(result);
		assertEquals("", result.getFirstName());
		assertEquals("", result.getLastName());
		assertEquals("", result.getUserGroup());
		assertNull(result.getUser());
		assertTrue(result.getTimeStamp() > 0);
	}

	@Test
	void testGeneratePermitDbIdWithAllParameters() {
		// Test generatePermitDbId method with all parameters
		String city = "Los Angeles";
		Integer laborCategoryCode = 123;
		Integer laborItem = 456;
		String zipCode = "90210";
		String county = "Los Angeles County";
		String municipality = "Beverly Hills";

		String result =
				PermitUtils.generatePermitDbId(city, laborCategoryCode, laborItem, zipCode, county, municipality);

		assertNotNull(result);
		assertFalse(result.isEmpty());

		// Verify it's valid Base64
		assertDoesNotThrow(() -> Base64.getUrlDecoder().decode(result));

		// Verify it can be decoded back
		PermitDbKey decodedKey = PermitUtils.decodePermitDbId(result);
		assertEquals("90210", decodedKey.getZipCode());
		assertEquals(123, decodedKey.getLaborCategoryCode());
		assertEquals(456, decodedKey.getLaborItem());
		assertEquals("Los Angeles", decodedKey.getCity());
		assertEquals("Los Angeles County", decodedKey.getCounty());
		assertEquals("Beverly Hills", decodedKey.getMunicipality());
	}

	@Test
	void testGeneratePermitDbIdWithNullIntegerParameters() {
		// Test generatePermitId method with null integer parameters
		String result = PermitUtils.generatePermitDbId(
				"Los Angeles", null, 456, "90210", "Los Angeles County", "Beverly Hills");

		assertNotNull(result);
		assertFalse(result.isEmpty());

		// Verify it's valid Base64
		assertDoesNotThrow(() -> Base64.getUrlDecoder().decode(result));

		// Verify it can be decoded back
		PermitDbKey decodedKey = PermitUtils.decodePermitDbId(result);
		assertEquals("Los Angeles", decodedKey.getCity());
		assertNull(decodedKey.getLaborCategoryCode());
		assertEquals(456, decodedKey.getLaborItem());
		assertEquals("90210", decodedKey.getZipCode());
		assertEquals("Los Angeles County", decodedKey.getCounty());
		assertEquals("Beverly Hills", decodedKey.getMunicipality());
	}

	@Test
	void testGeneratePermitDbIdWithSpecialCharacters() {
		// Test generatePermitDbId method with special characters
		String city = "City!@#$%^&*()";
		String zipCode = "90210!@#$";
		String county = "County!@#$";
		String municipality = "Municipality!@#$";

		String result = PermitUtils.generatePermitDbId(city, 123, 456, zipCode, county, municipality);

		assertNotNull(result);
		assertFalse(result.isEmpty());

		// Verify it's valid Base64
		assertDoesNotThrow(() -> Base64.getUrlDecoder().decode(result));

		// Verify it can be decoded back with normalized strings
		PermitDbKey decodedKey = PermitUtils.decodePermitDbId(result);
		assertEquals("City!@#$%^&*()", decodedKey.getCity());
		assertEquals("90210!@#$", decodedKey.getZipCode());
		assertEquals("County!@#$", decodedKey.getCounty());
		assertEquals("Municipality!@#$", decodedKey.getMunicipality());
	}

	@Test
	void testGeneratePermitDbIdWithUnicodeCharacters() {
		// Test generatePermitDbId method with unicode characters
		String city = "Ciudadñáéíóú";
		String zipCode = "Códigoñáéíóú";
		String county = "Condadonáéíóú";
		String municipality = "Municipalidadñáéíóú";

		String result = PermitUtils.generatePermitDbId(city, 123, 456, zipCode, county, municipality);

		assertNotNull(result);
		assertFalse(result.isEmpty());

		// Verify it's valid Base64
		assertDoesNotThrow(() -> Base64.getUrlDecoder().decode(result));

		// Verify it can be decoded back
		PermitDbKey decodedKey = PermitUtils.decodePermitDbId(result);
		assertEquals("Ciudadñáéíóú", decodedKey.getCity());
		assertEquals("Códigoñáéíóú", decodedKey.getZipCode());
		assertEquals("Condadonáéíóú", decodedKey.getCounty());
		assertEquals("Municipalidadñáéíóú", decodedKey.getMunicipality());
	}

	@Test
	void testGeneratePermitDbIdWithLongValues() {
		// Test generatePermitId method with long values
		String city = "Very Long City Name That Exceeds Normal Length Limits For Testing Purposes";
		String zipCode = "12345678901234567890";
		String county = "Very Long County Name That Exceeds Normal Length Limits";
		String municipality = "Very Long Municipality Name That Exceeds Normal Length Limits";

		String result = PermitUtils.generatePermitDbId(city, 123456789, 98765432, zipCode, county, municipality);

		assertNotNull(result);
		assertFalse(result.isEmpty());

		// Verify it's valid Base64
		assertDoesNotThrow(() -> Base64.getUrlDecoder().decode(result));
	}

	@Test
	void testDecodePermitDbIdWithValidId() {
		// Test decodePermitDbId method with valid ID
		String originalId =
				PermitUtils.generatePermitDbId("Los Angeles", 123, 456, "90210", "Los Angeles County", "Beverly Hills");

		PermitDbKey result = PermitUtils.decodePermitDbId(originalId);

		assertNotNull(result);
		assertEquals("90210", result.getZipCode());
		assertEquals(123, result.getLaborCategoryCode());
		assertEquals(456, result.getLaborItem());
		assertEquals("Los Angeles", result.getCity());
		assertEquals("Los Angeles County", result.getCounty());
		assertEquals("Beverly Hills", result.getMunicipality());
	}

	@Test
	void testDecodePermitDbIdWithNullId() {
		// Test decodePermitDbId method with null ID
		assertThrows(NullPointerException.class, () -> {
			PermitUtils.decodePermitDbId(null);
		});
	}

	@Test
	void testDecodePermitDbIdWithInvalidBase64() {
		// Test decodePermitDbId method with invalid Base64
		String invalidBase64 = "invalid-base64";

		assertThrows(IllegalArgumentException.class, () -> {
			PermitUtils.decodePermitDbId(invalidBase64);
		});
	}

	// Helper methods
	private String createValidUserToken() {
		try {
			ObjectMapper mapper = PermitUtils.getObjectMapper();
			String json = mapper.writeValueAsString(testUserToken);
			return Base64.getUrlEncoder().encodeToString(json.getBytes());
		} catch (Exception e) {
			throw new RuntimeException("Failed to create valid test token", e);
		}
	}

	private String createTokenWithNullFields() {
		try {
			ObjectMapper mapper = PermitUtils.getObjectMapper();
			UserToken tokenWithNulls = new UserToken();
			tokenWithNulls.setUser(null);
			tokenWithNulls.setFirstName(null);
			tokenWithNulls.setLastName(null);
			tokenWithNulls.setUserGroup(null);
			tokenWithNulls.setTimeStamp(System.currentTimeMillis());
			String json = mapper.writeValueAsString(tokenWithNulls);
			return Base64.getUrlEncoder().encodeToString(json.getBytes());
		} catch (Exception e) {
			throw new RuntimeException("Failed to create token with null fields", e);
		}
	}

	private String createTokenWithEmptyFields() {
		try {
			ObjectMapper mapper = PermitUtils.getObjectMapper();
			UserToken tokenWithEmptyFields = new UserToken();
			tokenWithEmptyFields.setFirstName("");
			tokenWithEmptyFields.setLastName("");
			tokenWithEmptyFields.setUserGroup("");
			tokenWithEmptyFields.setUser(null);
			tokenWithEmptyFields.setTimeStamp(System.currentTimeMillis());
			String json = mapper.writeValueAsString(tokenWithEmptyFields);
			return Base64.getUrlEncoder().encodeToString(json.getBytes());
		} catch (Exception e) {
			throw new RuntimeException("Failed to create token with empty fields", e);
		}
	}
}
