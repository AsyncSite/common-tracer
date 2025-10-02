package com.asyncsite.tracing.logtrace;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TraceIdTest {

    @Test
    void createTraceId_shouldInitializeWithIdAndLevelZero() {
        // given
        String testId = "abc12345";

        // when
        TraceId traceId = new TraceId(testId);

        // then
        assertThat(traceId.getId()).isEqualTo(testId);
        assertThat(traceId.getLevel()).isZero();
        assertThat(traceId.isFirstLevel()).isTrue();
    }

    @Test
    void createNextId_shouldIncrementLevel() {
        // given
        TraceId traceId = new TraceId("abc12345");

        // when
        TraceId nextId = traceId.createNextId();

        // then
        assertThat(nextId.getId()).isEqualTo(traceId.getId());
        assertThat(nextId.getLevel()).isEqualTo(1);
        assertThat(nextId.isFirstLevel()).isFalse();
    }

    @Test
    void createPreviousId_shouldDecrementLevel() {
        // given
        TraceId traceId = new TraceId("abc12345");
        TraceId nextId = traceId.createNextId();

        // when
        TraceId previousId = nextId.createPreviousId();

        // then
        assertThat(previousId.getId()).isEqualTo(traceId.getId());
        assertThat(previousId.getLevel()).isZero();
        assertThat(previousId.isFirstLevel()).isTrue();
    }

    @Test
    void createNestedLevels_shouldMaintainSameTraceId() {
        // given
        TraceId level0 = new TraceId("abc12345");

        // when
        TraceId level1 = level0.createNextId();
        TraceId level2 = level1.createNextId();
        TraceId level3 = level2.createNextId();

        // then
        assertThat(level0.getId()).isEqualTo("abc12345");
        assertThat(level1.getId()).isEqualTo("abc12345");
        assertThat(level2.getId()).isEqualTo("abc12345");
        assertThat(level3.getId()).isEqualTo("abc12345");

        assertThat(level0.getLevel()).isEqualTo(0);
        assertThat(level1.getLevel()).isEqualTo(1);
        assertThat(level2.getLevel()).isEqualTo(2);
        assertThat(level3.getLevel()).isEqualTo(3);
    }
}
