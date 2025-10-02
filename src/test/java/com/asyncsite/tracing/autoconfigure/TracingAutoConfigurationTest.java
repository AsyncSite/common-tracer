package com.asyncsite.tracing.autoconfigure;

import com.asyncsite.tracing.interceptor.GlobalTraceHandler;
import com.asyncsite.tracing.interceptor.TracingInterceptor;
import com.asyncsite.tracing.logtrace.LogTracer;
import com.asyncsite.tracing.logtrace.MicrometerLogTracer;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.client.RestTemplateCustomizer;

import static org.assertj.core.api.Assertions.assertThat;

class TracingAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(TracingAutoConfiguration.class))
        .withBean(Tracer.class, () -> null); // Mock Tracer

    @Test
    void autoConfiguration_whenEnabled_shouldRegisterAllBeans() {
        contextRunner
            .withPropertyValues("asyncsite.tracing.enabled=true")
            .run(context -> {
                assertThat(context).hasSingleBean(LogTracer.class);
                assertThat(context).hasSingleBean(MicrometerLogTracer.class);
                assertThat(context).hasSingleBean(GlobalTraceHandler.class);
                assertThat(context).hasSingleBean(TracingInterceptor.class);
                assertThat(context).hasSingleBean(TracingProperties.class);
            });
    }

    @Test
    void autoConfiguration_whenDisabled_shouldNotRegisterBeans() {
        contextRunner
            .withPropertyValues("asyncsite.tracing.enabled=false")
            .run(context -> {
                assertThat(context).doesNotHaveBean(LogTracer.class);
                assertThat(context).doesNotHaveBean(GlobalTraceHandler.class);
            });
    }

    @Test
    void autoConfiguration_whenHttpDisabled_shouldNotRegisterHttpBeans() {
        contextRunner
            .withPropertyValues(
                "asyncsite.tracing.enabled=true",
                "asyncsite.tracing.http-enabled=false"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(LogTracer.class);
                assertThat(context).doesNotHaveBean(TracingInterceptor.class);
                assertThat(context).doesNotHaveBean(RestTemplateCustomizer.class);
            });
    }

    @Test
    void properties_shouldLoadDefaults() {
        contextRunner
            .run(context -> {
                TracingProperties properties = context.getBean(TracingProperties.class);

                assertThat(properties.isEnabled()).isTrue();
                assertThat(properties.isHttpEnabled()).isTrue();
                assertThat(properties.isKafkaEnabled()).isTrue();
                assertThat(properties.isLogRequestResponse()).isTrue();
                assertThat(properties.getPackages()).contains("com.asyncsite");
            });
    }

    @Test
    void properties_shouldAllowCustomization() {
        contextRunner
            .withPropertyValues(
                "asyncsite.tracing.enabled=false",
                "asyncsite.tracing.http-enabled=false",
                "asyncsite.tracing.log-request-response=false",
                "asyncsite.tracing.packages[0]=com.custom.package"
            )
            .run(context -> {
                TracingProperties properties = context.getBean(TracingProperties.class);

                assertThat(properties.isEnabled()).isFalse();
                assertThat(properties.isHttpEnabled()).isFalse();
                assertThat(properties.isLogRequestResponse()).isFalse();
                assertThat(properties.getPackages()).containsExactly("com.custom.package");
            });
    }
}
