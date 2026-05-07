package com.lowes.permits.logging;

import static com.lowes.permits.constants.ApplicationConstants.MDC_TRACE_ID_KEY;
import static com.lowes.permits.constants.ApplicationConstants.SPAN_ID_HEADER;
import static com.lowes.permits.constants.ApplicationConstants.TRACE_ID_HEADER;

import org.reactivestreams.Subscription;
import org.slf4j.MDC;

import reactor.core.CoreSubscriber;
import reactor.util.context.Context;

public class MdcContextLifter<T> implements CoreSubscriber<T> {
	private final CoreSubscriber<? super T> actual;

	public MdcContextLifter(CoreSubscriber<? super T> actual) {
		this.actual = actual;
	}

	@Override
	public void onSubscribe(Subscription s) {
		actual.onSubscribe(s);
	}

	@Override
	public void onNext(T t) {
		restoreMdc();
		try {
			actual.onNext(t);
		} finally {
			clearMdc();
		}
	}

	@Override
	public void onError(Throwable t) {
		restoreMdc();
		try {
			actual.onError(t);
		} finally {
			clearMdc();
		}
	}

	@Override
	public void onComplete() {
		restoreMdc();
		try {
			actual.onComplete();
		} finally {
			clearMdc();
		}
	}

	@Override
	public Context currentContext() {
		return actual.currentContext();
	}

	private void restoreMdc() {
		Context context = actual.currentContext();
		context.getOrEmpty(MDC_TRACE_ID_KEY).ifPresent(traceId -> MDC.put(MDC_TRACE_ID_KEY, traceId.toString()));

		context.getOrEmpty(TRACE_ID_HEADER).ifPresent(traceId -> MDC.put(TRACE_ID_HEADER, traceId.toString()));

		context.getOrEmpty(SPAN_ID_HEADER).ifPresent(traceId -> MDC.put(SPAN_ID_HEADER, traceId.toString()));
	}

	private void clearMdc() {
		MDC.remove(MDC_TRACE_ID_KEY);
		MDC.remove(TRACE_ID_HEADER);
		MDC.remove(SPAN_ID_HEADER);
	}
}
