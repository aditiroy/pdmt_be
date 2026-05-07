package com.lowes.permits.logging;

import static com.lowes.permits.constants.ApplicationConstants.MDC_TRACE_ID_KEY;
import static com.lowes.permits.constants.ApplicationConstants.SPAN_ID_HEADER;
import static com.lowes.permits.constants.ApplicationConstants.TRACE_ID_HEADER;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;
import reactor.util.context.Context;

@Component
public class TraceIdFilterConfig implements WebFilter {

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		String xB3TraceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
		String xB3SpanId = exchange.getRequest().getHeaders().getFirst(SPAN_ID_HEADER);
		String traceId = xB3TraceId;
		if (traceId == null || traceId.isEmpty()) {
			traceId = UUID.randomUUID().toString();
		}
		// Store in Reactor Context
		return chain.filter(exchange)
				.contextWrite(Context.of(
						MDC_TRACE_ID_KEY,
						traceId,
						TRACE_ID_HEADER,
						null == xB3TraceId ? "" : xB3TraceId,
						SPAN_ID_HEADER,
						null == xB3SpanId ? "" : xB3SpanId));
	}
}
