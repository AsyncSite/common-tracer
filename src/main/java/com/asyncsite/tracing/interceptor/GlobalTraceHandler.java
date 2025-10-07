package com.asyncsite.tracing.interceptor;

import com.asyncsite.tracing.logtrace.LogTracer;
import com.asyncsite.tracing.logtrace.TraceStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * GlobalTraceHandler - AOP aspect for automatic method tracing
 *
 * Responsibilities:
 * - Intercept all methods in configured packages
 * - Automatically log method entry/exit with LogTracer
 * - Log parameters and return values in JSON format (DEBUG level)
 * - Exception propagation with trace context
 *
 * Configuration:
 * - Target packages configured via TracingProperties
 * - Can be disabled via asyncsite.tracing.enabled=false
 *
 * Benefits:
 * - Zero-code tracing (just add dependency)
 * - Consistent logging format across services
 * - Visual call hierarchy in logs
 * - Rich parameter/return value logging for debugging
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class GlobalTraceHandler {

    private final LogTracer logTracer;

    /**
     * Intercept all public methods in application/adapter/domain packages
     *
     * Note: Pointcut is dynamically configured via TracingProperties
     * Default pattern: execution(* com.asyncsite..application..*(..)) || ...
     */
    @Around("applicationLayer() || adapterLayer() || domainLayer()")
    public Object execute(ProceedingJoinPoint joinPoint) throws Throwable {
        TraceStatus status = null;
        Object result = null;

        try {
            // Begin trace with method signature
            status = logTracer.begin(joinPoint.getSignature().toShortString());

            // Log parameters and return values for all methods
            result = logWithParameters(joinPoint, status);

            return result;
        } catch (Exception exception) {
            // Log exception with trace context
            if (status != null) {
                logTracer.exception(null, status, exception);
            }
            throw exception;
        }
    }

    /**
     * Log method parameters and return value for all methods
     */
    private Object logWithParameters(ProceedingJoinPoint joinPoint, TraceStatus status) throws Throwable {
        // Log incoming parameters (안전하게)
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0) {
            String paramsJson = com.asyncsite.tracing.logtrace.SafeJsonLogger.toJsonArray(args);
            log.debug("Params: {}", paramsJson);
        }

        // Execute method
        Object result = joinPoint.proceed();

        // Log return value (안전하게)
        if (result != null) {
            String returnJson = com.asyncsite.tracing.logtrace.SafeJsonLogger.toJson(result);
            log.debug("Return: {}", returnJson);
        }

        logTracer.end(result, status);
        return result;
    }


    /**
     * Pointcut: Application layer (Use Cases / Services)
     * Matches: com.asyncsite.*.application..* or com.asyncsite.*.*.application..*
     */
    @Pointcut("execution(* com.asyncsite..application..*.*(..))")
    public void applicationLayer() {
    }

    /**
     * Pointcut: Adapter layer (Controllers / Persistence)
     * Matches: com.asyncsite.*.adapter..* or com.asyncsite.*.*.adapter..*
     */
    @Pointcut("execution(* com.asyncsite..adapter..*.*(..))")
    public void adapterLayer() {
    }

    /**
     * Pointcut: Domain layer (Domain Services)
     * Matches: com.asyncsite.*.domain..* (only methods in service classes)
     */
    @Pointcut("execution(* com.asyncsite..domain..*.*(..))")
    public void domainLayer() {
    }
}
