package com.asyncsite.tracing.logtrace;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;

/**
 * SafeJsonLogger - 절대 실패하지 않는 JSON 로깅 유틸리티
 *
 * 안전성 보장:
 * 1. NPE 방지: null 안전 처리
 * 2. 순환 참조 방지: Jackson 설정
 * 3. Lazy 로딩 무시: Hibernate 모듈
 * 4. 크기 제한: maxLength로 truncate
 * 5. 4단계 Fallback: JSON → toString → className → "FAILED"
 *
 * 지원 타입:
 * - Java 8 날짜/시간: LocalDateTime, LocalDate, etc. (JSR310Module)
 * - Java 8 Optional: Optional, OptionalInt, OptionalLong (JDK8Module)
 * - JPA 엔티티: Lazy loading 무시 (Hibernate5Module)
 *
 * 사용법:
 * <pre>
 * String json = SafeJsonLogger.toJson(request);
 * log.info("Request: {}", json);
 * </pre>
 */
@Slf4j
public class SafeJsonLogger {

    private static final int DEFAULT_MAX_LENGTH = 10_000;
    private static final int DEFAULT_MAX_DEPTH = 10;
    private static final String TRUNCATED_SUFFIX = "...[TRUNCATED]";
    private static final String FAILED_MESSAGE = "[LOGGING_FAILED]";

    private static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();

        // 실패 방지 설정
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        objectMapper.disable(SerializationFeature.FAIL_ON_SELF_REFERENCES);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        // 날짜 포맷
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // private 필드도 직렬화
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        objectMapper.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
        objectMapper.setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE);

        // 기타 안전 설정
        objectMapper.configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true);

        // Java 8 날짜/시간 타입 지원 (LocalDateTime, LocalDate 등)
        try {
            Class.forName("com.fasterxml.jackson.datatype.jsr310.JavaTimeModule");
            com.fasterxml.jackson.databind.Module jsr310Module = createJsr310Module();
            if (jsr310Module != null) {
                objectMapper.registerModule(jsr310Module);
                log.debug("JavaTimeModule registered for Java 8 date/time support");
            }
        } catch (ClassNotFoundException e) {
            log.debug("JavaTimeModule not found, skipping");
        } catch (Exception e) {
            log.debug("Failed to register JavaTimeModule: {}", e.getMessage());
        }

        // Java 8 Optional 타입 지원 (Optional, OptionalInt, OptionalLong 등)
        try {
            Class.forName("com.fasterxml.jackson.datatype.jdk8.Jdk8Module");
            com.fasterxml.jackson.databind.Module jdk8Module = createJdk8Module();
            if (jdk8Module != null) {
                objectMapper.registerModule(jdk8Module);
                log.debug("Jdk8Module registered for Java 8 Optional support");
            }
        } catch (ClassNotFoundException e) {
            log.debug("Jdk8Module not found, skipping");
        } catch (Exception e) {
            log.debug("Failed to register Jdk8Module: {}", e.getMessage());
        }

        // Hibernate Lazy Loading 무시 (의존성 있을 때만 활성화)
        try {
            Class.forName("com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module");
            com.fasterxml.jackson.databind.Module hibernateModule = createHibernateModule();
            if (hibernateModule != null) {
                objectMapper.registerModule(hibernateModule);
                log.debug("Hibernate5Module registered for safe lazy loading handling");
            }
        } catch (ClassNotFoundException e) {
            log.debug("Hibernate5Module not found, skipping (this is OK if not using JPA)");
        } catch (Exception e) {
            log.debug("Failed to register Hibernate5Module: {}", e.getMessage());
        }
    }

    /**
     * 객체를 JSON 문자열로 안전하게 변환
     *
     * @param obj 변환할 객체 (null 가능)
     * @return JSON 문자열 (절대 null 반환 안 함)
     */
    public static String toJson(Object obj) {
        return toJson(obj, DEFAULT_MAX_LENGTH);
    }

    /**
     * 객체를 JSON 문자열로 안전하게 변환 (길이 제한)
     *
     * @param obj 변환할 객체
     * @param maxLength 최대 길이
     * @return JSON 문자열
     */
    public static String toJson(Object obj, int maxLength) {
        // Level 0: null 체크
        if (obj == null) {
            return "null";
        }

        try {
            // Level 1: JSON 직렬화 시도
            String json = objectMapper.writeValueAsString(obj);
            return truncate(json, maxLength);

        } catch (Exception e) {
            log.trace("JSON serialization failed for {}: {}", obj.getClass().getName(), e.getMessage());

            try {
                // Level 2: toString() 시도
                String str = obj.toString();
                return truncate("[toString] " + str, maxLength);

            } catch (Exception e2) {
                log.trace("toString() failed for {}: {}", obj.getClass().getName(), e2.getMessage());

                try {
                    // Level 3: Class name + hashCode
                    return obj.getClass().getSimpleName() + "@" + Integer.toHexString(obj.hashCode());

                } catch (Exception e3) {
                    // Level 4: 최종 방어선
                    return FAILED_MESSAGE;
                }
            }
        }
    }

    /**
     * 배열을 JSON 문자열로 안전하게 변환
     *
     * @param args 배열
     * @return JSON 배열 문자열
     */
    public static String toJsonArray(Object[] args) {
        if (args == null) {
            return "null";
        }

        if (args.length == 0) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(toJson(args[i], DEFAULT_MAX_LENGTH / args.length));
        }
        sb.append("]");

        return truncate(sb.toString(), DEFAULT_MAX_LENGTH);
    }

    /**
     * 문자열을 최대 길이로 자르기
     */
    private static String truncate(String str, int maxLength) {
        if (str == null) {
            return "null";
        }

        if (str.length() <= maxLength) {
            return str;
        }

        return str.substring(0, maxLength - TRUNCATED_SUFFIX.length()) + TRUNCATED_SUFFIX;
    }

    /**
     * JavaTimeModule 동적 생성 (리플렉션)
     */
    private static com.fasterxml.jackson.databind.Module createJsr310Module() {
        try {
            Class<?> moduleClass = Class.forName("com.fasterxml.jackson.datatype.jsr310.JavaTimeModule");
            Object module = moduleClass.getDeclaredConstructor().newInstance();
            return (com.fasterxml.jackson.databind.Module) module;
        } catch (Exception e) {
            log.warn("Failed to create JavaTimeModule: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Jdk8Module 동적 생성 (리플렉션)
     */
    private static com.fasterxml.jackson.databind.Module createJdk8Module() {
        try {
            Class<?> moduleClass = Class.forName("com.fasterxml.jackson.datatype.jdk8.Jdk8Module");
            Object module = moduleClass.getDeclaredConstructor().newInstance();
            return (com.fasterxml.jackson.databind.Module) module;
        } catch (Exception e) {
            log.warn("Failed to create Jdk8Module: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Hibernate5Module 동적 생성 (리플렉션)
     */
    private static com.fasterxml.jackson.databind.Module createHibernateModule() {
        try {
            Class<?> moduleClass = Class.forName("com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module");
            Object module = moduleClass.getDeclaredConstructor().newInstance();

            // FORCE_LAZY_LOADING = false 설정
            Class<?> featureClass = Class.forName("com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module$Feature");
            Object forceLazyLoading = featureClass.getField("FORCE_LAZY_LOADING").get(null);

            moduleClass.getMethod("configure", featureClass, boolean.class)
                .invoke(module, forceLazyLoading, false);

            return (com.fasterxml.jackson.databind.Module) module;
        } catch (Exception e) {
            log.warn("Failed to configure Hibernate5Module: {}", e.getMessage());
            return null;
        }
    }
}
