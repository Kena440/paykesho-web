package org.paykesho.backoffice.config;

import lombok.RequiredArgsConstructor;
import org.paykesho.backoffice.idempotency.interceptor.IdempotencyInterceptor;
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
                .addPathPatterns("/**")
                .excludePathPatterns("/actuator/**", "/health/**", "/login/**", "/logout/**", "/auth/**", 
                                   "/css/**", "/js/**", "/images/**", "/assets/**",
                                   "/forgot-password/**", "/reset-password/**");
    }
}

