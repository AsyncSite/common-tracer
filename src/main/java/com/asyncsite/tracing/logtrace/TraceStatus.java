package com.asyncsite.tracing.logtrace;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Trace execution status value object
 *
 * Responsibilities:
 * - Holds current trace context
 * - Tracks start time for duration calculation
 * - Contains execution message
 */
@Getter
@AllArgsConstructor
public class TraceStatus {

    private final TraceId traceId;
    private final Long startTimeMs;
    private final String message;
}
