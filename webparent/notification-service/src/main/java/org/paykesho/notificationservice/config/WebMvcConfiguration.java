package org.paykesho.notificationservice.config;

import lombok.RequiredArgsConstructor;
import org.paykesho.notificationservice.idempotency.interceptor.IdempotencyInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfiguration implements WebMvcConfigurer {

    private final IdempotencyInterceptor idempotencyInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(idempotencyInterceptor)
                .addPathPatterns("/api/**", "/rest/api/**")
                .excludePathPatterns("/actuator/**", "/health/**", "/login/**", "/logout/**", "/auth/**",
                                   "/css/**", "/js/**", "/images/**", "/assets/**",
                                   "/forgot-password/**", "/reset-password/**");
    }
}

