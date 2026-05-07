package com.lowes.permits.mapper;

import static com.lowes.permits.constants.ApplicationConstants.DUMMY_VALUE;
import static com.lowes.permits.constants.ApplicationConstants.SYSTEM;
import static com.lowes.permits.constants.ApplicationConstants.UNKNOWN_APPLICATION;

import java.time.Instant;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import com.lowes.permits.dto.request.CreatePermitRequest;
import com.lowes.permits.dto.request.DeletePermitRequest;
import com.lowes.permits.dto.request.UpdatePermitRequest;
import com.lowes.permits.dto.response.ActivityResponse;
import com.lowes.permits.dto.response.ItemResponse;
import com.lowes.permits.dto.response.LaborItemResponse;
import com.lowes.permits.dto.response.OrderModResponse;
import com.lowes.permits.dto.response.PermitResponse;
import com.lowes.permits.entity.LaborItemMongoEntity;
import com.lowes.permits.entity.OrderModMongoEntity;
import com.lowes.permits.entity.PermitMongoEntity;
import com.lowes.permits.entity.PermitPostgresEntity;
import com.lowes.permits.model.Address;
import com.lowes.permits.model.Audit;
import com.lowes.permits.model.LaborCategory;
import com.lowes.permits.model.MappingContext;
import com.lowes.permits.model.PermitDbKey;
import com.lowes.permits.model.Provider;
import com.lowes.permits.model.UserToken;
import com.lowes.permits.util.PermitUtils;

@Mapper(componentModel = "spring")
public interface PermitMapper {

	@Named("generatePermitDbId")
	static String generatePermitDbId(CreatePermitRequest request) {
		String city = normalizeCity(request.getAddress().getCity());
		String county = normalizeCounty(request.getAddress().getCounty());
		String municipality = normalizeMunicipality(request.getAddress().getMunicipality());
		String zip = StringUtils.trim(request.getAddress().getZipCode());

		return PermitUtils.generatePermitDbId(
				city, request.getLaborCategory().getCode(), request.getLaborItem(), zip, county, municipality);
	}

	@Named("buildAddressForCreate")
	static Address buildAddressForCreate(CreatePermitRequest request) {
		Address address = new Address();
		address.setCity(normalizeCity(request.getAddress().getCity()));
		address.setState(StringUtils.trim(request.getAddress().getState()));
		address.setZipCode(StringUtils.trim(request.getAddress().getZipCode()));
		address.setCounty(normalizeCounty(request.getAddress().getCounty()));
		address.setMunicipality(normalizeMunicipality(request.getAddress().getMunicipality()));
		return address;
	}

	private static String normalizeCity(String value) {
		return StringUtils.upperCase(StringUtils.trim(value));
	}

	private static String normalizeMunicipality(String value) {
		String trimmed = StringUtils.trimToNull(value);
		return trimmed == null ? DUMMY_VALUE : StringUtils.upperCase(trimmed);
	}

	private static String normalizeCounty(String value) {
		if (value == null) {
			return DUMMY_VALUE;
		}
		String countyName = value.trim();
		if (countyName.toLowerCase().endsWith("county")) {
			countyName = countyName.substring(0, countyName.length() - 6).trim();
		}
		if (StringUtils.isBlank(countyName)) {
			return DUMMY_VALUE;
		}
		return countyName.toUpperCase();
	}

	@Named("buildProviderForCreate")
	static Provider buildProviderForCreate(CreatePermitRequest request) {
		if (request.getProvider() == null) {
			return null;
		}
		if (StringUtils.isBlank(request.getProvider().getName())
				&& request.getProvider().getNumber() == null) {
			return null;
		}
		return request.getProvider();
	}

	static Audit buildAuditFromContext(MappingContext context) {
		Audit audit = new Audit();
		UserToken token = PermitUtils.parseUserToken(context.getXUserToken());
		audit.setCreatedAt(Instant.now().toEpochMilli());
		audit.setLastModifiedAt(Instant.now().toEpochMilli());
		if (token != null) {
			audit.setCreatedByUserGroup(token.getUserGroup());
			audit.setCreatedByUserRole(token.getUser() != null ? token.getUser().getUserRole() : null);
			audit.setLastModifiedByUserGroup(token.getUserGroup());
			audit.setLastModifiedByUserRole(
					token.getUser() != null ? token.getUser().getUserRole() : null);
			audit.setCreatedById(token.getUser() != null ? token.getUser().getId() : null);
			audit.setCreatedByEmailId(token.getUser() != null ? token.getUser().getEmail() : null);
			audit.setCreatedByJobCode(token.getUser() != null ? token.getUser().getJobCode() : null);
			audit.setLastModifiedByEmailId(
					token.getUser() != null ? token.getUser().getEmail() : null);
			audit.setLastModifiedByJobCode(
					token.getUser() != null ? token.getUser().getJobCode() : null);
			audit.setLastModifiedById(token.getUser() != null ? token.getUser().getId() : null);
			audit.setCreatedByName(Optional.of(Stream.of(token.getFirstName(), token.getLastName())
							.filter(name -> name != null && !name.isBlank())
							.collect(Collectors.joining(" ")))
					.filter(name -> !name.isBlank())
					.orElse(null));
			audit.setLastModifiedByName(Optional.of(Stream.of(token.getFirstName(), token.getLastName())
							.filter(name -> name != null && !name.isBlank())
							.collect(Collectors.joining(" ")))
					.filter(name -> !name.isBlank())
					.orElse(null));
		} else {
			audit.setCreatedByName(SYSTEM);
			audit.setLastModifiedByName(SYSTEM);
		}
		if (StringUtils.isNotEmpty(context.getCallerApp())) {
			audit.setLastModifiedByApplicationName(context.getCallerApp());
			audit.setCreatedByApplicationName(context.getCallerApp());
		} else {
			audit.setLastModifiedByApplicationName(UNKNOWN_APPLICATION);
			audit.setCreatedByApplicationName(UNKNOWN_APPLICATION);
		}
		audit.setLastModifiedByTraceId(context.getCurrentTraceId());
		return audit;
	}

	@Named("buildAddressFromPermitDbId")
	static Address buildAddressFromPermitDbId(String permitDbId) {
		PermitDbKey key = PermitUtils.decodePermitDbId(permitDbId);
		Address address = new Address();
		address.setCity(key.getCity());
		address.setZipCode(key.getZipCode());
		address.setCounty(key.getCounty());
		address.setMunicipality(key.getMunicipality());
		return address;
	}

	@Named("buildLaborCategoryFromPermitDbId")
	static LaborCategory buildLaborCategoryFromPermitDbId(String permitDbId) {
		PermitDbKey key = PermitUtils.decodePermitDbId(permitDbId);
		LaborCategory laborCategory = new LaborCategory();
		laborCategory.setCode(key.getLaborCategoryCode());
		return laborCategory;
	}

	@Named("extractLaborItemFromPermitDbId")
	static Integer extractLaborItemFromPermitDbId(String permitDbId) {
		PermitDbKey key = PermitUtils.decodePermitDbId(permitDbId);
		return key.getLaborItem();
	}

	@Named("generatePermitResponseDbId")
	static String generatePermitResponseDbId(PermitPostgresEntity permitPostgresEntityResponse) {
		return PermitUtils.generatePermitDbId(
				permitPostgresEntityResponse.getCity(),
				permitPostgresEntityResponse.getLaborCategoryCode(),
				toInteger(permitPostgresEntityResponse.getLaborItem()),
				permitPostgresEntityResponse.getZipcode(),
				permitPostgresEntityResponse.getCounty(),
				permitPostgresEntityResponse.getMunicipality());
	}

	@Named("stringToInteger")
	static Integer toInteger(String value) {
		return Optional.ofNullable(value).map(Integer::valueOf).orElse(null);
	}

	@Named("trimString")
	static String trim(String value) {
		return Optional.ofNullable(value).map(String::trim).orElse(null);
	}

	@Named("trimAndNullifyDummy")
	static String trimAndNullifyDummy(String value) {
		String trimmed = Optional.ofNullable(value).map(String::trim).orElse(null);
		return DUMMY_VALUE.equals(trimmed) ? null : trimmed;
	}

	@Named("normalizeEstPermitObtainDays")
	static Integer normalizeEstPermitObtainDays(Integer value) {
		return value == null || value < 1 ? null : value;
	}

	@Named("parseOrderModTimestamp")
	static java.time.LocalDateTime parseOrderModTimestamp(String value) {
		if (value == null || value.isBlank()) return null;
		try {
			return java.time.LocalDateTime.parse(
					value, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		} catch (java.time.format.DateTimeParseException e) {
			return java.time.LocalDateTime.parse(value);
		}
	}

	@Named("parseIntSafe")
	static Integer parseIntSafe(String value) {
		if (value == null || value.isBlank()) return null;
		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	@Named("parseBigDecimalSafe")
	static java.math.BigDecimal parseBigDecimalSafe(String value) {
		if (value == null || value.isBlank()) return null;
		try {
			return new java.math.BigDecimal(value.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	ActivityResponse toActivityResponse(PermitMongoEntity entity);

	@AfterMapping
	default void nullifyDummyValuesInActivityResponse(@MappingTarget ActivityResponse response) {
		if (response.getAddress() != null) {
			if (DUMMY_VALUE.equalsIgnoreCase(response.getAddress().getCounty())) {
				response.getAddress().setCounty(null);
			}
			if (DUMMY_VALUE.equalsIgnoreCase(response.getAddress().getMunicipality())) {
				response.getAddress().setMunicipality(null);
			}
		}
	}

	@Mapping(target = "laborCategoryCode", source = "categoryCode")
	@Mapping(target = "laborCategoryDescription", source = "categoryDesc")
	@Mapping(target = "laborItem", source = "itemId", qualifiedByName = "parseIntSafe")
	@Mapping(target = "laborItemDescription", source = "itemDesc")
	@Mapping(target = "unitPermitFee", source = "permitFee", qualifiedByName = "parseBigDecimalSafe")
	@Mapping(target = "omniItemId", source = "omniId")
	@Mapping(target = "address.addressLine1", source = "address.addressLine1")
	@Mapping(target = "address.addressLine2", source = "address.addressLine2")
	@Mapping(target = "address.city", source = "address.city", qualifiedByName = "trimString")
	@Mapping(target = "address.state", source = "address.state", qualifiedByName = "trimString")
	@Mapping(target = "address.country", source = "address.country", qualifiedByName = "trimString")
	@Mapping(target = "address.zipCode", source = "address.zipCode", qualifiedByName = "trimString")
	@Mapping(target = "address.county", source = "address.county", qualifiedByName = "trimAndNullifyDummy")
	@Mapping(target = "address.municipality", source = "address.municipality", qualifiedByName = "trimAndNullifyDummy")
	@Mapping(target = "address.matchedAddress", source = "address.matchedAddress")
	@Mapping(target = "createdTimestamp", source = "createdTimestamp", qualifiedByName = "parseOrderModTimestamp")
	@Mapping(target = "lastUpdatedTimestamp", source = "updatedTimestamp", qualifiedByName = "parseOrderModTimestamp")
	@Mapping(target = "oldPermitFee", source = "oldPermitFee", qualifiedByName = "parseBigDecimalSafe")
	@Mapping(target = "createdBy", ignore = true)
	@Mapping(target = "audit", source = "audit")
	OrderModResponse toOrderModResponse(OrderModMongoEntity entity);

	@Mapping(target = "id", source = ".", qualifiedByName = "generatePermitResponseDbId")
	@Mapping(target = "laborItem", source = "laborItem", qualifiedByName = "stringToInteger")
	@Mapping(target = "laborItemDescription", source = "laborItemDescription")
	@Mapping(target = "unitPermitFee", source = "unitPermitFee")
	@Mapping(target = "omniItemId", source = "omniItemId")
	@Mapping(target = "laborCategory.code", source = "laborCategoryCode")
	@Mapping(target = "laborCategory.description", source = "laborCategoryDescription")
	@Mapping(target = "address.municipality", source = "municipality", qualifiedByName = "trimAndNullifyDummy")
	@Mapping(target = "address.city", source = "city", qualifiedByName = "trimString")
	@Mapping(target = "address.state", source = "state", qualifiedByName = "trimString")
	@Mapping(target = "address.county", source = "county", qualifiedByName = "trimAndNullifyDummy")
	@Mapping(target = "address.zipCode", source = "zipcode", qualifiedByName = "trimString")
	@Mapping(target = "provider.name", source = "provider")
	@Mapping(target = "provider.number", source = "vbuNumber", qualifiedByName = "stringToInteger")
	@Mapping(target = "audit.createdByName", source = "createdBy")
	@Mapping(target = "audit.lastModifiedByName", source = "updatedBy")
	@Mapping(target = "estPermitObtainDays", source = "estPermitObtainDays")
	PermitResponse toPermitResponse(PermitPostgresEntity permitPostgresEntity);

	@Mapping(source = ".", target = "permitDbId", qualifiedByName = "generatePermitDbId")
	@Mapping(target = "audit", expression = "java(PermitMapper.buildAuditFromContext(context))")
	@Mapping(source = ".", target = "address", qualifiedByName = "buildAddressForCreate")
	@Mapping(source = ".", target = "provider", qualifiedByName = "buildProviderForCreate")
	@Mapping(
			source = "estPermitObtainDays",
			target = "estPermitObtainDays",
			qualifiedByName = "normalizeEstPermitObtainDays")
	@Mapping(target = "operationType", expression = "java(context.getOperationType())")
	@Mapping(target = "status", expression = "java(context.getStatus())")
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "oldUnitPermitFee", ignore = true)
	@Mapping(target = "oldEstPermitObtainDays", ignore = true)
	@Mapping(target = "retryCount", ignore = true)
	@Mapping(target = "errorMessage", ignore = true)
	PermitMongoEntity createRequestToEntity(CreatePermitRequest request, @Context MappingContext context);

	@Mapping(source = "permitDbId", target = "permitDbId")
	@Mapping(target = "audit", expression = "java(PermitMapper.buildAuditFromContext(context))")
	@Mapping(target = "operationType", expression = "java(context.getOperationType())")
	@Mapping(target = "status", expression = "java(context.getStatus())")
	@Mapping(target = "laborItem", source = "permitDbId", qualifiedByName = "extractLaborItemFromPermitDbId")
	@Mapping(source = "request.laborCategory", target = "laborCategory")
	@Mapping(target = "address", source = "permitDbId", qualifiedByName = "buildAddressFromPermitDbId")
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "laborItemDescription", ignore = true)
	@Mapping(target = "oldUnitPermitFee", ignore = true)
	@Mapping(target = "omniItemId", ignore = true)
	@Mapping(target = "estPermitObtainDays", ignore = true)
	@Mapping(target = "oldEstPermitObtainDays", ignore = true)
	@Mapping(target = "provider", ignore = true)
	@Mapping(target = "retryCount", ignore = true)
	@Mapping(target = "errorMessage", ignore = true)
	@Mapping(target = "unitPermitFee", source = "request.unitPermitFee")
	PermitMongoEntity deleteRequestToEntity(DeletePermitRequest request, @Context MappingContext context);

	@Mapping(target = "audit", expression = "java(PermitMapper.buildAuditFromContext(context))")
	@Mapping(target = "operationType", expression = "java(context.getOperationType())")
	@Mapping(target = "status", expression = "java(context.getStatus())")
	@Mapping(source = "request.oldUnitPermitFee", target = "oldUnitPermitFee")
	@Mapping(source = "request.unitPermitFee", target = "unitPermitFee")
	@Mapping(source = "request.permitDbId", target = "permitDbId")
	@Mapping(source = "request.laborCategory", target = "laborCategory")
	@Mapping(source = "permitDbId", target = "laborItem", qualifiedByName = "extractLaborItemFromPermitDbId")
	@Mapping(source = "permitDbId", target = "address", qualifiedByName = "buildAddressFromPermitDbId")
	@Mapping(
			source = "request.estPermitObtainDays",
			target = "estPermitObtainDays",
			qualifiedByName = "normalizeEstPermitObtainDays")
	@Mapping(
			source = "request.oldEstPermitObtainDays",
			target = "oldEstPermitObtainDays",
			qualifiedByName = "normalizeEstPermitObtainDays")
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "laborItemDescription", ignore = true)
	@Mapping(target = "omniItemId", ignore = true)
	@Mapping(target = "provider", ignore = true)
	@Mapping(target = "retryCount", ignore = true)
	@Mapping(target = "errorMessage", ignore = true)
	PermitMongoEntity updateRequestToEntity(UpdatePermitRequest request, @Context MappingContext context);

	default LaborItemResponse mapToLaborItemResponse(LaborItemMongoEntity entity, String errorMessage) {
		LaborItemResponse response = new LaborItemResponse();
		response.setLaborItem(entity.getLaborItem());
		response.setLaborItemDescription(entity.getLaborItemDescription());
		response.setOmniItemId(entity.getOmniItemId());
		response.setErrorMessage(errorMessage);
		if (entity.getLaborCategory() != null) {
			LaborItemResponse.LaborCategory laborCategory = new LaborItemResponse.LaborCategory();
			laborCategory.setCode(entity.getLaborCategory().getCode());
			laborCategory.setDescription(entity.getLaborCategory().getDescription());
			response.setLaborCategory(laborCategory);
		}
		return response;
	}

	default LaborItemMongoEntity mapToLaborItemMongoEntity(ItemResponse.Product product) {
		LaborItemMongoEntity entity = new LaborItemMongoEntity();
		entity.setLaborItem(product.getItemNumber());
		entity.setLaborItemDescription(product.getLaborDetails().getLaborDescription());
		entity.setOmniItemId(product.getOmniItemId());
		entity.setSource("BIFROST");
		entity.setCreatedAt(Instant.now().toEpochMilli());
		entity.setLastModifiedAt(Instant.now().toEpochMilli());

		if (product.getLaborDetails() != null && product.getLaborDetails().getLaborCgyCode() != null) {
			LaborItemMongoEntity.LaborCategory laborCategory = new LaborItemMongoEntity.LaborCategory();
			laborCategory.setCode(Integer.parseInt(product.getLaborDetails().getLaborCgyCode()));
			laborCategory.setDescription(product.getLaborDetails().getLaborCgyDesc());
			entity.setLaborCategory(laborCategory);
		}

		return entity;
	}
}
