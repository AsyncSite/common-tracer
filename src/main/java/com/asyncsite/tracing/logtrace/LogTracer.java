package com.asyncsite.tracing.logtrace;

/**
 * LogTracer interface for distributed tracing with visual log indentation
 *
 * Responsibilities:
 * - Begin trace for method execution
 * - End trace with result
 * - Handle exceptions with trace context
 *
 * Usage:
 * <pre>
 * TraceStatus status = logTracer.begin("OrderService.createOrder()");
 * try {
 *     Order result = orderService.createOrder(request);
 *     logTracer.end(result, status);
 *     return result;
 * } catch (Exception e) {
 *     logTracer.exception(null, status, e);
 *     throw e;
 * }
 * </pre>
 */
public interface LogTracer {

    /**
     * Begin tracing a method execution
     *
     * @param message Method signature or execution context (e.g., "OrderService.createOrder()")
     * @return TraceStatus containing trace context and start time
     */
    TraceStatus begin(String message);

    /**
     * End tracing with successful result
     *
     * @param result Method return value (can be null)
     * @param status TraceStatus from begin()
     */
    void end(Object result, TraceStatus status);

    /**
     * End tracing with exception
     *
     * @param result Partial result if any
     * @param status TraceStatus from begin()
     * @param e Exception that occurred
     */
    void exception(Object result, TraceStatus status, Exception e);
}
