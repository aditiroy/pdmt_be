package com.lowes.permits.logging;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.contrib.json.classic.JsonLayout;
import lombok.SneakyThrows;

public class CustomJsonLayout extends JsonLayout {

	public static final String MASKED_ATTRIBUTES_ENTITY = "logging.masked.entity";
	public static final String MASKED_ATTRIBUTES_EXTRA = "logging.masked.extra";
	private static final String currentEnv = System.getProperty("spring.profiles.active", "unknown");
	private static final String MDC_TRACE_ID_KEY = "x-b3-traceid";
	private static final String MDC_SPAN_ID_KEY = "x-b3-spanid";
	private final Pattern multilinePattern;

	/** On application startup, load up entity that should be masked into logger. */
	@SneakyThrows
	public CustomJsonLayout() {
		var configuration = loadProperties();
		// Load property "logging.masked.entity" from application.properties
		var maskedEntity = configuration.get(MASKED_ATTRIBUTES_ENTITY);
		var maskPatterns = initMaskedPatterns(configuration.get(MASKED_ATTRIBUTES_EXTRA));
		// If the property is not null then we will parse the parent classes to be
		// masked and add them to a pattern
		if (maskedEntity != null) {
			var maskedEntities = maskedEntity.toString().split(",");
			for (var entity : maskedEntities) {
				var classType = Class.forName(entity.trim());
				MaskingUtil.parseFieldsToPattern(classType.getDeclaredFields(), maskPatterns);
			}
		}
		multilinePattern = Pattern.compile(String.join("|", maskPatterns), Pattern.MULTILINE);
	}

	@Override
	public String doLayout(ILoggingEvent iLoggingEvent) {
		var maskedMessage = maskMessage(super.doLayout(iLoggingEvent));
		maskedMessage = fixErrorLogging(iLoggingEvent, maskedMessage);
		return maskedMessage;
	}

	/**
	 * Read every log message and mask based on generated patterns
	 *
	 * @param message is every message being logged
	 * @return masked message
	 */
	private String maskMessage(String message) {
		var sb = new StringBuilder(message);
		var matcher = multilinePattern.matcher(sb);

		// Scan message to see if it contains any of the patterns we're searching for
		while (matcher.find()) {
			IntStream.rangeClosed(1, matcher.groupCount())
					.forEach(group -> MaskingUtil.detailedMasking(sb, matcher, group));
		}
		return sb.toString();
	}

	/**
	 * Fixes logging for level ERROR, makes stack trace readable; not in one line
	 *
	 * @param message Logged message being checked for level ERROR
	 * @return message String where if is of level ERROR then make stack trace readable
	 */
	private String fixErrorLogging(ILoggingEvent iLoggingEvent, String message) {
		// If level is of "Error", add newline to exception log, replace string of \t,
		// \r, and \n with real ones.
		if (currentEnv.toLowerCase().contains("local")
				&& iLoggingEvent.getLevel().equals(Level.ERROR)) {
			var exceptionIndex = message.indexOf("\"exception\"");
			message = new StringBuilder(message).insert(exceptionIndex, "\n").toString();
			message = message.replace("\\t", "\t");
			message = message.replace("\\r", "\r");
			message = message.replace("\\n", "\n");
		}
		return message;
	}

	/**
	 * Initialize extra attributes to be masked from property "logging.masked.extra"
	 *
	 * @param extraMasking is a property value from application.properties
	 * @return a list of extra patterns with masking regex
	 */
	private List<String> initMaskedPatterns(Object extraMasking) {
		var patterns = new LinkedList<String>();
		if (extraMasking != null) {
			for (var splitString : extraMasking.toString().split(",")) {
				patterns.add(splitString.trim());
			}
		}
		return MaskingUtil.maskAdditionalFields(patterns);
	}

	/** Load property file values from application.properties */
	private Properties loadProperties() throws IOException {
		var configuration = new Properties();
		var inputStream = CustomJsonLayout.class.getClassLoader().getResourceAsStream("application.properties");
		configuration.load(inputStream);
		inputStream.close();
		return configuration;
	}

	@Override
	protected void addCustomDataToJsonMap(Map<String, Object> map, ILoggingEvent event) {
		super.addCustomDataToJsonMap(map, event);

		// Extract x-b3-traceid and x-b3-spanid from MDC and place them at the root
		// level
		if (event.getMDCPropertyMap().containsKey(MDC_TRACE_ID_KEY)) {
			map.put(MDC_TRACE_ID_KEY, event.getMDCPropertyMap().get(MDC_TRACE_ID_KEY));
			map.put("x_b3_trace_id", event.getMDCPropertyMap().get(MDC_TRACE_ID_KEY));
		}
		if (event.getMDCPropertyMap().containsKey(MDC_SPAN_ID_KEY)) {
			map.put(MDC_SPAN_ID_KEY, event.getMDCPropertyMap().get(MDC_SPAN_ID_KEY));
			map.put("x_b3_span_id", event.getMDCPropertyMap().get(MDC_SPAN_ID_KEY));
		}
	}
}
