package com.asyncsite.tracing.interceptor;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * TracingInterceptor - HTTP client interceptor for trace propagation
 *
 * Responsibilities:
 * - Inject B3 tracing headers into outgoing HTTP requests
 * - Propagate TraceId/SpanId across service boundaries
 * - Compatible with Zipkin/Jaeger if configured
 *
 * Headers added:
 * - X-B3-TraceId: Current trace ID
 * - X-B3-SpanId: Current span ID
 * - X-B3-Sampled: Sampling decision (1 = sampled, 0 = not sampled)
 *
 * Usage:
 * - Auto-configured for RestTemplate when starter is included
 * - Can be manually added to WebClient/RestClient
 *
 * Example:
 * <pre>
 * Query Daily → [X-B3-TraceId: abc123] → Checkout
 *               [X-B3-SpanId: def456]
 * </pre>
 */
@Slf4j
@RequiredArgsConstructor
public class TracingInterceptor implements ClientHttpRequestInterceptor {

    private final Tracer tracer;

    @Override
    public ClientHttpResponse intercept(
        HttpRequest request,
        byte[] body,
        ClientHttpRequestExecution execution
    ) throws IOException {

        Span currentSpan = tracer.currentSpan();

        if (currentSpan != null && currentSpan.context() != null) {
            // Add B3 propagation headers
            String traceId = currentSpan.context().traceId();
            String spanId = currentSpan.context().spanId();

            if (traceId != null && !traceId.isEmpty()) {
                request.getHeaders().set("X-B3-TraceId", traceId);
            }

            if (spanId != null && !spanId.isEmpty()) {
                request.getHeaders().set("X-B3-SpanId", spanId);
            }

            // Sampling decision
            request.getHeaders().set("X-B3-Sampled", "1");

            log.debug("Propagating trace: traceId={}, spanId={}, uri={}",
                traceId, spanId, request.getURI());
        }

        return execution.execute(request, body);
    }
}
