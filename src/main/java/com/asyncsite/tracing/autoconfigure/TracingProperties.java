package com.asyncsite.tracing.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for AsyncSite Tracing
 *
 * Usage in application.yml:
 * <pre>
 * asyncsite:
 *   tracing:
 *     enabled: true
 *     log-request-response: true
 *     packages:
 *       - com.asyncsite.querydailyservice
 *       - com.asyncsite.checkoutservice
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "asyncsite.tracing")
public class TracingProperties {

    /**
     * Enable/disable distributed tracing
     * Default: true
     */
    private boolean enabled = true;

    /**
     * Enable detailed logging for RestController request/response bodies
     * Default: true
     */
    private boolean logRequestResponse = true;

    /**
     * Target packages to trace
     * Default: com.asyncsite
     *
     * Example:
     * - com.asyncsite.querydailyservice
     * - com.asyncsite.checkoutservice
     */
    private List<String> packages = new ArrayList<>(List.of("com.asyncsite"));

    /**
     * Enable Kafka message header propagation
     * Default: true (if spring-kafka is on classpath)
     */
    private boolean kafkaEnabled = true;

    /**
     * Enable HTTP header propagation (X-B3-TraceId, X-B3-SpanId)
     * Default: true
     */
    private boolean httpEnabled = true;
}
