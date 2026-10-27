package org.paykesho.paymentservice.security;

import org.springframework.http.HttpMethod;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    private final RateLimitingFilter rateLimitingFilter;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                .csrf(csrf -> csrf.disable())
                .authorizeRequests(authz -> authz
                        .antMatchers("/rest/api/**", "/actuator/health", "/actuator/health/**").permitAll()
                        // Authenticated by the endpoints themselves: the settlement API checks the
                        // X-PayKesho-Internal-Token header, the Lenco webhook checks X-Lenco-Signature.
                        // Without these, Spring Security answered 403 and no merchant was ever paid.
                        .antMatchers(HttpMethod.POST, "/internal/v1/settlements", "/internal/v1/settlements/reconcile",
                                "/webhooks/lenco").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );
    }
}
