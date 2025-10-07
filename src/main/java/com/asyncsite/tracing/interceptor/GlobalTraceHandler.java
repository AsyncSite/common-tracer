package com.asyncsite.tracing.interceptor;

import com.asyncsite.tracing.logtrace.LogTracer;
import com.asyncsite.tracing.logtrace.TraceStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * GlobalTraceHandler - AOP aspect for automatic method tracing
 *
 * Responsibilities:
 * - Intercept all methods in configured packages
 * - Automatically log method entry/exit with LogTracer
 * - Special handling for @RestController (log request/response bodies)
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

            // Special handling for REST controllers (log request/response)
            if (isAnnotationPresent(joinPoint, RestController.class)) {
                result = logWithParameters(joinPoint, status);
            } else {
                result = joinPoint.proceed();
                logTracer.end(result, status);
            }

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
     * Log incoming request and outgoing response for REST controllers
     */
    private Object logWithParameters(ProceedingJoinPoint joinPoint, TraceStatus status) throws Throwable {
        // Log incoming request parameters (안전하게)
        String requestJson = com.asyncsite.tracing.logtrace.SafeJsonLogger.toJsonArray(joinPoint.getArgs());
        log.info("Request: {}", requestJson);

        // Execute method
        Object result = joinPoint.proceed();

        // Log outgoing response (안전하게)
        String responseJson = com.asyncsite.tracing.logtrace.SafeJsonLogger.toJson(result);
        log.info("Response: {}", responseJson);

        logTracer.end(result, status);
        return result;
    }

    /**
     * Check if target class has specified annotation
     */
    @SafeVarargs
    private static boolean isAnnotationPresent(
        ProceedingJoinPoint joinPoint,
        Class<? extends Annotation>... annotationClasses
    ) {
        Class<?> targetClass = getTargetClass(joinPoint);
        return Arrays.stream(annotationClasses)
            .anyMatch(targetClass::isAnnotationPresent);
    }

    /**
     * Get target class from ProceedingJoinPoint
     */
    private static Class<?> getTargetClass(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        return method.getDeclaringClass();
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
