package com.lowes.permits.logging;

import static com.lowes.permits.constants.ApplicationConstants.MDC_TRACE_ID_KEY;
import static com.lowes.permits.constants.ApplicationConstants.SPAN_ID_HEADER;
import static com.lowes.permits.constants.ApplicationConstants.TRACE_ID_HEADER;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Operators;

@Component
@Order(1)
public class GlobalTraceInterceptor {

	@PostConstruct
	public void init() {
		Hooks.onEachOperator(MDC_TRACE_ID_KEY, Operators.lift((sc, sub) -> new MdcContextLifter<>(sub)));
		Hooks.onEachOperator(TRACE_ID_HEADER, Operators.lift((sc, sub) -> new MdcContextLifter<>(sub)));
		Hooks.onEachOperator(SPAN_ID_HEADER, Operators.lift((sc, sub) -> new MdcContextLifter<>(sub)));
	}

	@PreDestroy
	public void cleanup() {
		Hooks.resetOnEachOperator(MDC_TRACE_ID_KEY);
		Hooks.resetOnEachOperator(TRACE_ID_HEADER);
		Hooks.resetOnEachOperator(SPAN_ID_HEADER);
	}
}
