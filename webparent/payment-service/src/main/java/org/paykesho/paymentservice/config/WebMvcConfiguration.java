package org.paykesho.paymentservice.config;

import lombok.RequiredArgsConstructor;
import org.paykesho.paymentservice.idempotency.interceptor.IdempotencyInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration for payment service
 * Registers the idempotency interceptor
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfiguration implements WebMvcConfigurer {

    private final IdempotencyInterceptor idempotencyInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(idempotencyInterceptor)
                .addPathPatterns("/rest/api/**", "/api/**") // Apply to all API endpoints
                .excludePathPatterns(
                    "/actuator/**",
                    "/health/**",
                    "/metrics/**",
                    "/login/**",
                    "/logout/**",
                    "/auth/**",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/assets/**",
                    "/forgot-password/**",
                    "/reset-password/**"
                );
    }
}

