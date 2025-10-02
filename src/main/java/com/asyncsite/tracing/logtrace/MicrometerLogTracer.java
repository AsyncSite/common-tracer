package com.asyncsite.tracing.logtrace;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * MicrometerLogTracer - Distributed tracing with Micrometer + LogTracer integration
 *
 * Key Design:
 * - TraceId: From Micrometer's currentSpan (distributed across services)
 * - Level: ThreadLocal for call depth indentation (service-local)
 * - Logging: Same visual format as hhplus LogTracer
 *
 * Differences from ThreadLocalLogTracer:
 * - TraceId source: Micrometer Tracer (not UUID)
 * - Propagation: Automatic via HTTP/Kafka headers (B3 format)
 * - Compatibility: Works with Zipkin/Jaeger if configured
 *
 * Usage:
 * - Auto-configured when starter is included
 * - Can be used manually or via GlobalTraceHandler AOP
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MicrometerLogTracer implements LogTracer {

    private static final String START_PREFIX = "-->";
    private static final String COMPLETE_PREFIX = "<--";
    private static final String EXCEPTION_PREFIX = "<X-";

    private final Tracer tracer;

    private final ThreadLocal<Integer> levelHolder = ThreadLocal.withInitial(() -> 0);
    private final ThreadLocal<Boolean> isExceptionLogged = ThreadLocal.withInitial(() -> false);

    @Override
    public TraceStatus begin(String message) {
        isExceptionLogged.set(false);

        String traceId = getCurrentTraceId();
        int level = getCurrentLevel();

        Long startTimeMs = System.currentTimeMillis();
        log.info("[{}] {}{}", traceId, addSpace(START_PREFIX, level), message);

        // Increase level for nested calls
        levelHolder.set(level + 1);

        return new TraceStatus(new TraceId(traceId), startTimeMs, message);
    }

    @Override
    public void end(Object result, TraceStatus status) {
        complete(result, status, null);
    }

    @Override
    public void exception(Object result, TraceStatus status, Exception exception) {
        if (!isExceptionLogged.get()) {
            log.info("[{}] {}{} time={}ms ex={}",
                status.getTraceId().getId(),
                addSpace(EXCEPTION_PREFIX, status.getTraceId().getLevel()),
                limitMessage(status.getMessage()),
                System.currentTimeMillis() - status.getStartTimeMs(),
                exception.toString());
            isExceptionLogged.set(true);
        }
        releaseLevel();
    }

    /**
     * Get current TraceId with priority:
     * 1. MDC correlationId (from Gateway/existing system)
     * 2. Micrometer traceId (distributed tracing)
     * 3. Generate new UUID (fallback)
     *
     * This ensures compatibility with existing correlationId infrastructure
     * while adding Micrometer distributed tracing capabilities.
     */
    private String getCurrentTraceId() {
        // Priority 1: Use existing correlationId from MDC (Gateway sets this)
        String correlationId = org.slf4j.MDC.get("correlationId");
        if (correlationId != null && !correlationId.isEmpty()) {
            // Truncate to 8 chars for consistency
            return correlationId.substring(0, Math.min(correlationId.length(), 8));
        }

        // Priority 2: Use Micrometer traceId (cross-service propagation)
        Span currentSpan = tracer.currentSpan();
        if (currentSpan != null && currentSpan.context() != null) {
            String traceId = currentSpan.context().traceId();
            if (traceId != null && !traceId.isEmpty()) {
                return traceId.substring(0, Math.min(traceId.length(), 8));
            }
        }

        // Priority 3: Generate new TraceId (fallback)
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Get current indentation level
     */
    private int getCurrentLevel() {
        Integer level = levelHolder.get();
        return level != null ? level : 0;
    }

    /**
     * Complete trace logging with timing
     */
    private void complete(Object result, TraceStatus status, Exception exception) {
        long resultTimeMs = System.currentTimeMillis() - status.getStartTimeMs();
        TraceId traceId = status.getTraceId();

        if (exception == null) {
            log.info("[{}] {}{} time={}ms",
                traceId.getId(),
                addSpace(COMPLETE_PREFIX, traceId.getLevel()),
                limitMessage(status.getMessage()),
                resultTimeMs);
        } else {
            log.info("[{}] {}{} time={}ms ex={}",
                traceId.getId(),
                addSpace(EXCEPTION_PREFIX, traceId.getLevel()),
                limitMessage(status.getMessage()),
                resultTimeMs,
                exception.toString());
        }

        releaseLevel();
    }

    /**
     * Decrease level after method completion
     */
    private void releaseLevel() {
        int currentLevel = getCurrentLevel();
        int newLevel = Math.max(0, currentLevel - 1);

        if (newLevel == 0) {
            levelHolder.remove();
        } else {
            levelHolder.set(newLevel);
        }
    }

    /**
     * Limit message length to prevent log overflow
     */
    private static String limitMessage(String message) {
        if (message == null) {
            return null;
        }
        return message.substring(0, Math.min(message.length(), 2000));
    }

    /**
     * Create indentation spacing for visual hierarchy
     *
     * Level 0: |-->
     * Level 1: |   |-->
     * Level 2: |   |   |-->
     */
    private static String addSpace(String prefix, int level) {
        return IntStream.range(0, level + 1)
            .mapToObj(i -> (i == level) ? "|" + prefix : "|   ")
            .collect(Collectors.joining());
    }
}
