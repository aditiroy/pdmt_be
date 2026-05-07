package com.lowes.permits.logging;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.stream.IntStream;

import com.lowes.permits.constants.ApplicationConstants;

import jakarta.validation.Valid;
import lombok.SneakyThrows;

public class MaskingUtil {

	private static final List<Object[]> annotationValues = new ArrayList<>();

	/**
	 * Parses given classes' fields to obtain fields annotated with @LogSensitive
	 *
	 * @param fields is all the fields of the current class being parsed
	 */
	@SneakyThrows
	public static void parseFieldsToPattern(Field[] fields, List<String> maskPatterns) {
		for (Field field : fields) {
			// If annotated with @Valid inspect that field's fields
			if (field.isAnnotationPresent(Valid.class)) {
				Class<?> clazz = field.getType();
				// Extract class information from list
				if (field.getType().equals(List.class)) {
					// Contains class name of List and type within list
					var stringListType = (ParameterizedType) field.getGenericType();
					// removes "class " prefix from name to generate class
					clazz = Class.forName(stringListType
							.getActualTypeArguments()[0]
							.toString()
							.substring(6));
				}
				parseFieldsToPattern(clazz.getDeclaredFields(), maskPatterns);
			}
			// If annotated with @LogSensitive add keyword to pattern matcher
			if (field.isAnnotationPresent(LogSensitive.class) && !maskPatterns.contains(fixPattern(field.getName()))) {
				maskPatterns.add(fixPattern(field.getName()));
				annotationValues.add(addLogSensitiveValues(field));
			}
		}
	}

	/**
	 * Scan Annotation of current field and write values to array
	 *
	 * @param field is current annotated field being read
	 * @return Object[] containing key of masking format and values of masking amount
	 */
	private static Object[] addLogSensitiveValues(Field field) {
		var log = field.getAnnotation(LogSensitive.class);
		Object[] annotatedValues = new Object[3];

		annotatedValues[0] = log.type().toString();
		annotatedValues[1] = log.ignoreStart();
		annotatedValues[2] = log.ignoreEnd();
		return annotatedValues;
	}

	/**
	 * Masks log messages
	 *
	 * @param sb contains message to be printed in logs
	 * @param matcher contains patterns to be masked
	 * @param group the current group number of pattern being inspected
	 */
	public static void detailedMasking(StringBuilder sb, Matcher matcher, int group) {
		// If there is no match on current pattern being examined ignore if statement
		if (matcher.group(group) != null) {
			// Store index of starting and ending character for currently evaluated
			// sensitive information
			int start = matcher.start(group);
			int end = matcher.end(group);
			int ignoreStart = 0;
			int ignoreEnd = 0;
			if ((int) annotationValues.get(group - 1)[1] != 0) {
				ignoreStart = (int) annotationValues.get(group - 1)[1];
			}
			if ((int) annotationValues.get(group - 1)[2] != 0) {
				ignoreEnd = (int) annotationValues.get(group - 1)[2];
			}

			switch (LogSensitive.Type.valueOf(annotationValues.get(group - 1)[0].toString())) {
				case CUSTOM:
					start += ignoreStart;
					end -= ignoreEnd;
					break;
				case PHONENUMBER:
					start += determineIgnoreStart(ignoreStart, ApplicationConstants.IGNORE_START_FOR_PHONENUMBER);
					end -= determineIgnoreEnd(ignoreEnd, ApplicationConstants.IGNORE_END_FOR_PHONENUMBER);
					break;
				case CREDITCARD:
					start += determineIgnoreStart(ignoreStart, ApplicationConstants.IGNORE_START_FOR_CREDITCARD);
					end -= determineIgnoreEnd(ignoreEnd, ApplicationConstants.IGNORE_END_FOR_CREDITCARD);
					break;
				case EMAIL:
					start += determineIgnoreStart(ignoreStart, ApplicationConstants.IGNORE_START_FOR_EMAIL);
					// end starts right before the @ symbol to expose the email domain
					end = matcher.start(group)
							+ matcher.group(group).indexOf("@")
							- determineIgnoreEnd(ignoreEnd, ApplicationConstants.IGNORE_END_FOR_EMAIL);
					break;
			}

			// if LogSensitive ignoreStart and/or ignoreEnd exceeds character count of the
			// value, mask whole value
			if (start > end) {
				start = matcher.start(group);
				end = matcher.end(group);
			}
			IntStream.range(start, end).forEach(i -> sb.setCharAt(i, '*'));
		}
	}

	/**
	 * Adds additional regex to fix pattern matching. Finds name/value pair then extracts only value
	 *
	 * @param propertyName is the name of the field used to match pattern for masking
	 * @return fixed pattern regex
	 */
	private static String fixPattern(String propertyName) {
		return ApplicationConstants.MASKING_PATTERN_REGEX_PREFIX
				+ propertyName
				+ ApplicationConstants.MASKING_PATTERN_REGEX_SUFFIX;
	}

	/**
	 * Determine how many characters should be ignored from the start of the sensitive information
	 *
	 * @param ignoreStart given ignoreStart value on the annotation LogSensitive
	 * @param ignoreStartConstant is the default value given in ApplicationConstants
	 */
	private static int determineIgnoreStart(int ignoreStart, int ignoreStartConstant) {
		if (ignoreStart == 0) {
			return ignoreStartConstant;
		} else {
			return ignoreStart;
		}
	}

	/**
	 * Determine how many characters should be ignored from the end of sensitive information
	 *
	 * @param ignoreEnd given ignoreEnd value on the annotation LogSensitive
	 * @param ignoreEndConstant is the default value given in ApplicationConstants
	 */
	private static int determineIgnoreEnd(int ignoreEnd, int ignoreEndConstant) {
		if (ignoreEnd == 0) {
			return ignoreEndConstant;
		} else {
			return ignoreEnd;
		}
	}

	/**
	 * Adds regex pattern to given list of strings
	 *
	 * @param additionalFields Fields outside of entity that needs to be masked
	 * @return a list of fields that needs to be masked with attached regex.
	 */
	public static List<String> maskAdditionalFields(List<String> additionalFields) {
		var maskedPatterns = new ArrayList<String>();
		for (var additionalField : additionalFields) {
			maskedPatterns.add(fixPattern(additionalField));
			Object[] annotatedValues = {LogSensitive.Type.CUSTOM, 0, 0};
			annotationValues.add(annotatedValues);
		}
		return maskedPatterns;
	}
}
