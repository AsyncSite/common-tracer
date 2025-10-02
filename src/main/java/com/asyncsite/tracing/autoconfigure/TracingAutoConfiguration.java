package com.asyncsite.tracing.autoconfigure;

import com.asyncsite.tracing.interceptor.GlobalTraceHandler;
import com.asyncsite.tracing.interceptor.TracingInterceptor;
import com.asyncsite.tracing.logtrace.LogTracer;
import com.asyncsite.tracing.logtrace.MicrometerLogTracer;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * TracingAutoConfiguration - Spring Boot auto-configuration for distributed tracing
 *
 * Auto-configures:
 * 1. MicrometerLogTracer (LogTracer implementation)
 * 2. GlobalTraceHandler (AOP aspect for automatic method tracing)
 * 3. TracingInterceptor (HTTP header propagation for RestTemplate)
 * 4. TracingProperties (configuration properties)
 *
 * Activation:
 * - Auto-activated when common-tracer is on classpath
 * - Can be disabled via: asyncsite.tracing.enabled=false
 *
 * Requirements:
 * - Micrometer Tracing on classpath
 * - Spring Boot 3.x
 * - AspectJ for AOP
 *
 * Usage:
 * <pre>
 * dependencies {
 *     implementation("com.asyncsite:common-tracer:0.1.0-SNAPSHOT")
 * }
 * </pre>
 */
@Slf4j
@AutoConfiguration
@EnableAspectJAutoProxy
@EnableConfigurationProperties(TracingProperties.class)
@ConditionalOnClass({Tracer.class, LogTracer.class})
@RequiredArgsConstructor
public class TracingAutoConfiguration {

    private final TracingProperties properties;

    /**
     * Configure MicrometerLogTracer as default LogTracer implementation
     */
    @Bean
    @ConditionalOnMissingBean(LogTracer.class)
    @ConditionalOnProperty(prefix = "asyncsite.tracing", name = "enabled", havingValue = "true", matchIfMissing = true)
    public LogTracer logTracer(Tracer tracer) {
        log.info("Auto-configuring MicrometerLogTracer with Micrometer Tracer");
        return new MicrometerLogTracer(tracer);
    }

    /**
     * Configure GlobalTraceHandler AOP aspect for automatic method tracing
     */
    @Bean
    @ConditionalOnMissingBean(GlobalTraceHandler.class)
    @ConditionalOnProperty(prefix = "asyncsite.tracing", name = "enabled", havingValue = "true", matchIfMissing = true)
    public GlobalTraceHandler globalTraceHandler(LogTracer logTracer) {
        log.info("Auto-configuring GlobalTraceHandler for packages: {}", properties.getPackages());
        return new GlobalTraceHandler(logTracer);
    }

    /**
     * Configure TracingInterceptor for HTTP trace propagation
     */
    @Bean
    @ConditionalOnMissingBean(TracingInterceptor.class)
    @ConditionalOnProperty(prefix = "asyncsite.tracing", name = "http-enabled", havingValue = "true", matchIfMissing = true)
    public TracingInterceptor tracingInterceptor(Tracer tracer) {
        log.info("Auto-configuring TracingInterceptor for HTTP trace propagation");
        return new TracingInterceptor(tracer);
    }

    /**
     * Configure RestTemplate customizer to add TracingInterceptor
     *
     * This ensures all RestTemplate beans get the tracing interceptor automatically
     */
    @Bean
    @ConditionalOnClass(RestTemplate.class)
    @ConditionalOnProperty(prefix = "asyncsite.tracing", name = "http-enabled", havingValue = "true", matchIfMissing = true)
    public RestTemplateCustomizer restTemplateTracingCustomizer(TracingInterceptor tracingInterceptor) {
        log.info("Auto-configuring RestTemplate with TracingInterceptor");

        return restTemplate -> {
            List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>(restTemplate.getInterceptors());
            interceptors.add(tracingInterceptor);
            restTemplate.setInterceptors(interceptors);
        };
    }

    /**
     * Log configuration summary on startup
     */
    @Bean
    @ConditionalOnProperty(prefix = "asyncsite.tracing", name = "enabled", havingValue = "true", matchIfMissing = true)
    public TracingConfigurationLogger tracingConfigurationLogger() {
        return new TracingConfigurationLogger(properties);
    }

    /**
     * Helper class to log tracing configuration on startup
     */
    @RequiredArgsConstructor
    static class TracingConfigurationLogger {
        private final TracingProperties properties;

        @jakarta.annotation.PostConstruct
        public void logConfiguration() {
            log.info("╔════════════════════════════════════════════════════════════╗");
            log.info("║  AsyncSite Distributed Tracing - ACTIVATED                ║");
            log.info("╠════════════════════════════════════════════════════════════╣");
            log.info("║  Enabled: {}", properties.isEnabled());
            log.info("║  HTTP Propagation: {}", properties.isHttpEnabled());
            log.info("║  Kafka Propagation: {}", properties.isKafkaEnabled());
            log.info("║  Log Request/Response: {}", properties.isLogRequestResponse());
            log.info("║  Target Packages: {}", properties.getPackages());
            log.info("╠════════════════════════════════════════════════════════════╣");
            log.info("║  Features:                                                 ║");
            log.info("║  ✓ Micrometer TraceId integration                         ║");
            log.info("║  ✓ Visual log indentation (LogTracer)                     ║");
            log.info("║  ✓ Automatic method tracing (AOP)                         ║");
            log.info("║  ✓ B3 header propagation (HTTP/Kafka)                     ║");
            log.info("║  ✓ Docker logs correlation (grep trace:xxx)               ║");
            log.info("╚════════════════════════════════════════════════════════════╝");
        }
    }
}
