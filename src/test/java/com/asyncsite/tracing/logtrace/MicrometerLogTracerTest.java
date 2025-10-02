package com.asyncsite.tracing.logtrace;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MicrometerLogTracerTest {

    @Mock
    private Tracer tracer;

    @Mock
    private Span span;

    @Mock
    private TraceContext traceContext;

    private MicrometerLogTracer logTracer;

    @BeforeEach
    void setUp() {
        logTracer = new MicrometerLogTracer(tracer);
    }

    @Test
    void begin_withMicrometerSpan_shouldUseTraceIdFromMicrometer() {
        // given
        String expectedTraceId = "1234567890abcdef";
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn(expectedTraceId);

        // when
        TraceStatus status = logTracer.begin("TestService.testMethod()");

        // then
        assertThat(status).isNotNull();
        assertThat(status.getTraceId().getId()).isEqualTo(expectedTraceId.substring(0, 8));
        assertThat(status.getMessage()).isEqualTo("TestService.testMethod()");
        assertThat(status.getStartTimeMs()).isNotNull();
    }

    @Test
    void begin_withoutMicrometerSpan_shouldGenerateNewTraceId() {
        // given
        when(tracer.currentSpan()).thenReturn(null);

        // when
        TraceStatus status = logTracer.begin("TestService.testMethod()");

        // then
        assertThat(status).isNotNull();
        assertThat(status.getTraceId().getId()).hasSize(8);
        assertThat(status.getMessage()).isEqualTo("TestService.testMethod()");
    }

    @Test
    void end_shouldCalculateExecutionTime() throws InterruptedException {
        // given
        when(tracer.currentSpan()).thenReturn(null);
        TraceStatus status = logTracer.begin("TestService.testMethod()");

        // when
        Thread.sleep(10); // Simulate some execution time
        logTracer.end("result", status);

        // then - should not throw exception
        assertThat(status.getStartTimeMs()).isLessThan(System.currentTimeMillis());
    }

    @Test
    void exception_shouldLogException() {
        // given
        when(tracer.currentSpan()).thenReturn(null);
        TraceStatus status = logTracer.begin("TestService.testMethod()");
        Exception testException = new RuntimeException("Test exception");

        // when
        logTracer.exception(null, status, testException);

        // then - should not throw exception
        assertThat(status).isNotNull();
    }

    @Test
    void nestedCalls_shouldMaintainTraceId() {
        // given
        String expectedTraceId = "abc12345";
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn(expectedTraceId);

        // when
        TraceStatus status1 = logTracer.begin("Level1.method()");
        TraceStatus status2 = logTracer.begin("Level2.method()");
        TraceStatus status3 = logTracer.begin("Level3.method()");

        // then - all should have same trace ID
        assertThat(status1.getTraceId().getId()).isEqualTo(expectedTraceId.substring(0, 8));
        assertThat(status2.getTraceId().getId()).isEqualTo(expectedTraceId.substring(0, 8));
        assertThat(status3.getTraceId().getId()).isEqualTo(expectedTraceId.substring(0, 8));

        // Cleanup
        logTracer.end(null, status3);
        logTracer.end(null, status2);
        logTracer.end(null, status1);
    }
}
