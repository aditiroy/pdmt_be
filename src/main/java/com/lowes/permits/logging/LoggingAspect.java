package com.lowes.permits.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

	@Around("@annotation(org.springframework.web.bind.annotation.PostMapping) || "
			+ "@annotation(org.springframework.web.bind.annotation.GetMapping) || "
			+ "@annotation(org.springframework.web.bind.annotation.PatchMapping) || "
			+ "@annotation(org.springframework.web.bind.annotation.PutMapping) || "
			+ "@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
	public Object logControllerResponseTime(ProceedingJoinPoint joinPoint) throws Throwable {
		long startTime = System.currentTimeMillis();
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		String methodName = signature.getMethod().getName();

		Object result = joinPoint.proceed();

		if (result instanceof Mono<?>) {
			return ((Mono<?>) result).doOnTerminate(() -> {
				long duration = System.currentTimeMillis() - startTime;
				log.info("Time taken to get the response for {} is {} ms", methodName, duration);
			});
		} else if (result instanceof Flux<?>) {
			return ((Flux<?>) result).doOnTerminate(() -> {
				long duration = System.currentTimeMillis() - startTime;
				log.info("Time taken to get the response for {} is {} ms", methodName, duration);
			});
		} else {
			long duration = System.currentTimeMillis() - startTime;
			log.info("Time taken to get the response for {} is {} ms", methodName, duration);
			return result;
		}
	}

	@Around("execution(public * com.lowes.permits.repository.PermitMongoRepository.*(..))")
	public Object logMongoResponseTime(ProceedingJoinPoint joinPoint) throws Throwable {
		return logDatabaseResponseTime(joinPoint, "MongoDB");
	}

	@Around("execution(public * com.lowes.permits.repository.PermitPostgresRepository.*(..))")
	public Object logPostgresResponseTime(ProceedingJoinPoint joinPoint) throws Throwable {
		return logDatabaseResponseTime(joinPoint, "PostgreSQL");
	}

	private Object logDatabaseResponseTime(ProceedingJoinPoint joinPoint, String dbType) throws Throwable {
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		String methodName = signature.getMethod().getName();
		String params = formatParameters(signature, joinPoint.getArgs());

		Object result = joinPoint.proceed();

		if (result instanceof Mono<?>) {
			return logMonoResponseTime((Mono<?>) result, dbType, methodName, params);
		} else if (result instanceof Flux<?>) {
			return logFluxResponseTime((Flux<?>) result, dbType, methodName, params);
		}

		return result;
	}

	private String formatParameters(MethodSignature signature, Object[] args) {
		if (args == null || args.length == 0) {
			return "no params";
		}

		String[] paramNames = signature.getParameterNames();
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < args.length; i++) {
			if (i > 0) {
				sb.append(", ");
			}
			String paramName = (paramNames != null && i < paramNames.length) ? paramNames[i] : "arg" + i;
			sb.append(paramName).append("=").append(formatValue(args[i]));
		}

		return sb.toString();
	}

	private String formatValue(Object value) {
		if (value == null) {
			return "null";
		}
		if (value instanceof String) {
			return "\"" + value + "\"";
		}
		if (value instanceof java.util.List<?>) {
			java.util.List<?> list = (java.util.List<?>) value;
			return "[" + list.size() + " items]";
		}
		if (value instanceof java.util.Map<?, ?>) {
			java.util.Map<?, ?> map = (java.util.Map<?, ?>) value;
			return "{" + map.size() + " entries}";
		}
		if (value.getClass().isArray()) {
			return "[array]";
		}
		return value.toString();
	}

	private <T> Mono<T> logMonoResponseTime(Mono<T> mono, String dbType, String operationName, String params) {
		long startTime = System.currentTimeMillis();
		return mono.doOnSuccess(result -> {
					long duration = System.currentTimeMillis() - startTime;
					String responseSize = getResponseSize(result);
					log.info(
							"{} operation '{}' completed in {} ms - params: [{}] - responseSize: {}",
							dbType,
							operationName,
							duration,
							params,
							responseSize);
				})
				.doOnError(error -> {
					long duration = System.currentTimeMillis() - startTime;
					log.error(
							"{} operation '{}' failed after {} ms - params: [{}] - error: {}",
							dbType,
							operationName,
							duration,
							params,
							error.getMessage());
				});
	}

	private <T> Flux<T> logFluxResponseTime(Flux<T> flux, String dbType, String operationName, String params) {
		long startTime = System.currentTimeMillis();
		java.util.concurrent.atomic.AtomicInteger count = new java.util.concurrent.atomic.AtomicInteger(0);

		return flux.doOnNext(item -> count.incrementAndGet())
				.doOnComplete(() -> {
					long duration = System.currentTimeMillis() - startTime;
					log.info(
							"{} operation '{}' completed in {} ms - params: [{}] - responseSize: {} ",
							dbType,
							operationName,
							duration,
							params,
							count.get());
				})
				.doOnError(error -> {
					long duration = System.currentTimeMillis() - startTime;
					log.error(
							"{} operation '{}' failed after {} ms - params: [{}] - error: {}",
							dbType,
							operationName,
							duration,
							params,
							error.getMessage());
				});
	}

	private String getResponseSize(Object result) {
		if (result == null) {
			return "null";
		}
		if (result instanceof java.util.List<?>) {
			java.util.List<?> list = (java.util.List<?>) result;
			return list.size() + " records";
		}
		if (result instanceof java.util.Collection<?>) {
			java.util.Collection<?> collection = (java.util.Collection<?>) result;
			return collection.size() + " records";
		}
		if (result instanceof java.util.Map<?, ?>) {
			java.util.Map<?, ?> map = (java.util.Map<?, ?>) result;
			return map.size() + " entries";
		}
		if (result.getClass().isArray()) {
			return java.lang.reflect.Array.getLength(result) + " records";
		}
		if (result instanceof Number) {
			return "count=" + result;
		}
		return "1 record";
	}
}
