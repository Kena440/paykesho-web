package org.paykesho.fileservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Security configuration for File Service
 * Allows public access to file operations but with rate limiting
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    private final RateLimitingFilter rateLimitingFilter;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            // Add rate limiting filter
            .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
            
            // Disable CSRF for REST API endpoints
            .csrf(csrf -> csrf.disable())
            
            // Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Configure authorization
            .authorizeRequests(authz -> authz
                // Allow public access to file operations
                .antMatchers("/uploads-api", "/access-api", "/uploads/**").permitAll()
                // Allow health checks
                .antMatchers("/actuator/health").permitAll()
                // Require authentication for actuator endpoints
                .antMatchers("/actuator/**").authenticated()
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            
            // Stateless session management (no sessions)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Security headers - relaxed for file serving
            .headers(headers -> headers
                .frameOptions().deny()
                .contentTypeOptions().and()
                .xssProtection().and()
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(31536000)
                    .includeSubDomains(true)
                )
                // Relax CSP to allow file access from external domains
                .contentSecurityPolicy("default-src 'self' 'unsafe-inline' data: blob: https:")
            );
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOriginPattern("*");
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/uploads/**", configuration);
        return source;
    }
}

