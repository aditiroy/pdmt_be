package com.lowes.permits.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowes.permits.exception.InvalidUserTokenException;
import com.lowes.permits.model.PermitDbKey;
import com.lowes.permits.model.UserToken;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class PermitUtils {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final String SEP = "\u001E";

	public static ObjectMapper getObjectMapper() {
		return OBJECT_MAPPER;
	}

	public static String resolveTraceId(String xB3TraceId) {
		return (xB3TraceId != null && !xB3TraceId.isBlank())
				? xB3TraceId
				: UUID.randomUUID().toString();
	}

	public static UserToken parseUserToken(String xUserToken) {
		try {
			byte[] decoded = Base64.getDecoder().decode(xUserToken);
			String json = new String(decoded, StandardCharsets.UTF_8);
			return PermitUtils.getObjectMapper().readValue(json, UserToken.class);
		} catch (Exception e) {
			throw new InvalidUserTokenException("Invalid X-USER-TOKEN header", e);
		}
	}

	public static String generatePermitDbId(
			String city,
			Integer laborCategoryCode,
			Integer laborItem,
			String zipCode,
			String county,
			String municipality) {

		String joined = String.join(
				SEP,
				normalizeString(zipCode),
				nullSafeToString(laborCategoryCode),
				nullSafeToString(laborItem),
				normalizeString(city),
				normalizeString(county),
				normalizeString(municipality));
		return Base64.getEncoder().withoutPadding().encodeToString(joined.getBytes(StandardCharsets.UTF_8));
	}

	public static PermitDbKey decodePermitDbId(String key) {
		String decoded = new String(Base64.getDecoder().decode(key), StandardCharsets.UTF_8);
		String[] splitDecodedKeys = decoded.split(SEP, -1);
		PermitDbKey permitDbKey = new PermitDbKey();
		permitDbKey.setZipCode(splitDecodedKeys[0]);
		permitDbKey.setLaborCategoryCode(parseInteger(splitDecodedKeys[1]));
		permitDbKey.setLaborItem(parseInteger(splitDecodedKeys[2]));
		permitDbKey.setCity(splitDecodedKeys[3]);
		permitDbKey.setCounty(splitDecodedKeys[4]);
		permitDbKey.setMunicipality(splitDecodedKeys[5]);

		return permitDbKey;
	}

	private static String nullSafeToString(Integer value) {
		return value == null ? null : value.toString();
	}

	private static String normalizeString(String value) {
		return StringUtils.isBlank(value) || "null".equalsIgnoreCase(value.trim()) ? null : value.trim();
	}

	private static Integer parseInteger(String value) {
		if (StringUtils.isBlank(value) || "null".equalsIgnoreCase(value.trim())) return null;

		try {
			return Integer.valueOf(value);
		} catch (NumberFormatException ex) {
			log.error("Invalid integer value in permit DB key: {} with exception {}", value, ex.getMessage());
		}
		return null;
	}
}
