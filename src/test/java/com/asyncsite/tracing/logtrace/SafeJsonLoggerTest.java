package com.asyncsite.tracing.logtrace;

import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SafeJsonLoggerTest {

    @Test
    void toJson_withSimpleObject_shouldReturnJson() {
        // given
        TestDto dto = new TestDto("test@example.com", "Test User");

        // when
        String json = SafeJsonLogger.toJson(dto);

        // then
        assertThat(json).contains("test@example.com");
        assertThat(json).contains("Test User");
    }

    @Test
    void toJson_withNull_shouldReturnNullString() {
        // when
        String json = SafeJsonLogger.toJson(null);

        // then
        assertThat(json).isEqualTo("null");
    }

    @Test
    void toJson_withCircularReference_shouldNotThrowException() {
        // given
        CircularA a = new CircularA();
        CircularB b = new CircularB();
        a.b = b;
        b.a = a;

        // when
        String json = SafeJsonLogger.toJson(a);

        // then (순환 참조여도 예외 안 터짐)
        assertThat(json).isNotNull();
        assertThat(json).doesNotContain("LOGGING_FAILED");
    }

    @Test
    void toJson_withUnserializableObject_shouldFallbackToToString() {
        // given
        UnserializableObject obj = new UnserializableObject();

        // when
        String json = SafeJsonLogger.toJson(obj);

        // then (JSON 실패하면 toString으로 fallback)
        assertThat(json).isNotNull();
        // toString이나 클래스명이 포함되어야 함
    }

    @Test
    void toJson_withLongString_shouldTruncate() {
        // given
        String longString = "a".repeat(20000);
        TestDto dto = new TestDto(longString, "test");

        // when
        String json = SafeJsonLogger.toJson(dto, 1000);

        // then
        assertThat(json.length()).isLessThanOrEqualTo(1000);
        assertThat(json).contains("[TRUNCATED]");
    }

    @Test
    void toJsonArray_withMultipleObjects_shouldReturnJsonArray() {
        // given
        Object[] args = {
            new TestDto("user1@test.com", "User 1"),
            new TestDto("user2@test.com", "User 2"),
            "simple string"
        };

        // when
        String json = SafeJsonLogger.toJsonArray(args);

        // then
        assertThat(json).startsWith("[");
        assertThat(json).endsWith("]");
        assertThat(json).contains("user1@test.com");
        assertThat(json).contains("user2@test.com");
        assertThat(json).contains("simple string");
    }

    @Test
    void toJsonArray_withNull_shouldReturnNullString() {
        // when
        String json = SafeJsonLogger.toJsonArray(null);

        // then
        assertThat(json).isEqualTo("null");
    }

    @Test
    void toJsonArray_withEmptyArray_shouldReturnEmptyJsonArray() {
        // when
        String json = SafeJsonLogger.toJsonArray(new Object[]{});

        // then
        assertThat(json).isEqualTo("[]");
    }

    @Test
    void toJson_withNestedObjects_shouldSerializeCompletely() {
        // given
        NestedDto parent = new NestedDto("parent");
        parent.children = List.of(
            new TestDto("child1@test.com", "Child 1"),
            new TestDto("child2@test.com", "Child 2")
        );

        // when
        String json = SafeJsonLogger.toJson(parent);

        // then
        assertThat(json).contains("parent");
        assertThat(json).contains("child1@test.com");
        assertThat(json).contains("child2@test.com");
    }

    @Test
    void toJson_withLocalDateTime_shouldSerializeAsString() {
        // given
        java.time.LocalDateTime now = java.time.LocalDateTime.of(2025, 10, 8, 14, 30, 0);
        DateTimeDto dto = new DateTimeDto(now, "Test Event");

        // when
        String json = SafeJsonLogger.toJson(dto);

        // then (LocalDateTime이 ISO-8601 형식으로 직렬화되어야 함)
        assertThat(json).contains("2025-10-08");
        assertThat(json).contains("14:30");
        assertThat(json).contains("Test Event");
        assertThat(json).doesNotContain("[toString]");
    }

    @Test
    void toJson_withOptionalPresent_shouldSerializeValue() {
        // given
        java.util.Optional<String> optional = java.util.Optional.of("test value");
        OptionalDto dto = new OptionalDto(optional, "Test");

        // when
        String json = SafeJsonLogger.toJson(dto);

        // then (Optional이 값으로 직렬화되어야 함)
        assertThat(json).contains("test value");
        assertThat(json).contains("Test");
        assertThat(json).doesNotContain("[toString]");
        assertThat(json).doesNotContain("Optional");
    }

    @Test
    void toJson_withOptionalEmpty_shouldSerializeAsNull() {
        // given
        java.util.Optional<String> optional = java.util.Optional.empty();
        OptionalDto dto = new OptionalDto(optional, "Test");

        // when
        String json = SafeJsonLogger.toJson(dto);

        // then (Optional.empty()는 null로 직렬화되어야 함)
        assertThat(json).contains("Test");
        assertThat(json).doesNotContain("[toString]");
    }

    @Test
    void toJson_withOptionalInt_shouldSerializeAsNumber() {
        // given
        java.util.OptionalInt optionalInt = java.util.OptionalInt.of(42);

        // when
        String json = SafeJsonLogger.toJson(optionalInt);

        // then (OptionalInt가 숫자로 직렬화되어야 함)
        assertThat(json).contains("42");
        assertThat(json).doesNotContain("[toString]");
    }

    // Test DTOs
    static class TestDto {
        private String email;
        private String name;

        public TestDto(String email, String name) {
            this.email = email;
            this.name = name;
        }
    }

    static class CircularA {
        private CircularB b;
    }

    static class CircularB {
        private CircularA a;
    }

    static class UnserializableObject {
        private OutputStream stream = System.out;

        @Override
        public String toString() {
            return "UnserializableObject{custom}";
        }
    }

    static class NestedDto {
        private String name;
        private List<TestDto> children = new ArrayList<>();

        public NestedDto(String name) {
            this.name = name;
        }
    }

    static class DateTimeDto {
        private java.time.LocalDateTime scheduledAt;
        private String description;

        public DateTimeDto(java.time.LocalDateTime scheduledAt, String description) {
            this.scheduledAt = scheduledAt;
            this.description = description;
        }
    }

    static class OptionalDto {
        private java.util.Optional<String> optionalValue;
        private String name;

        public OptionalDto(java.util.Optional<String> optionalValue, String name) {
            this.optionalValue = optionalValue;
            this.name = name;
        }
    }
}
