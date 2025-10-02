package com.asyncsite.tracing.interceptor;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;

import java.io.IOException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TracingInterceptorTest {

    @Mock
    private Tracer tracer;

    @Mock
    private Span span;

    @Mock
    private TraceContext traceContext;

    @Mock
    private ClientHttpRequestExecution execution;

    @Mock
    private ClientHttpResponse response;

    private TracingInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new TracingInterceptor(tracer);
    }

    @Test
    void intercept_withActiveSpan_shouldAddB3Headers() throws IOException {
        // given
        String traceId = "1234567890abcdef";
        String spanId = "abcdef1234567890";

        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn(traceId);
        when(traceContext.spanId()).thenReturn(spanId);
        when(execution.execute(any(), any())).thenReturn(response);

        HttpRequest request = new MockClientHttpRequest();
        byte[] body = new byte[0];

        // when
        interceptor.intercept(request, body, execution);

        // then
        assertThat(request.getHeaders().get("X-B3-TraceId")).containsExactly(traceId);
        assertThat(request.getHeaders().get("X-B3-SpanId")).containsExactly(spanId);
        assertThat(request.getHeaders().get("X-B3-Sampled")).containsExactly("1");

        verify(execution).execute(request, body);
    }

    @Test
    void intercept_withoutActiveSpan_shouldNotAddHeaders() throws IOException {
        // given
        when(tracer.currentSpan()).thenReturn(null);
        when(execution.execute(any(), any())).thenReturn(response);

        HttpRequest request = new MockClientHttpRequest();
        byte[] body = new byte[0];

        // when
        interceptor.intercept(request, body, execution);

        // then
        assertThat(request.getHeaders().get("X-B3-TraceId")).isNull();
        assertThat(request.getHeaders().get("X-B3-SpanId")).isNull();
        assertThat(request.getHeaders().get("X-B3-Sampled")).isNull();

        verify(execution).execute(request, body);
    }

    @Test
    void intercept_withNullContext_shouldNotAddHeaders() throws IOException {
        // given
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(null);
        when(execution.execute(any(), any())).thenReturn(response);

        HttpRequest request = new MockClientHttpRequest();
        byte[] body = new byte[0];

        // when
        interceptor.intercept(request, body, execution);

        // then
        assertThat(request.getHeaders().get("X-B3-TraceId")).isNull();
        verify(execution).execute(request, body);
    }
}
