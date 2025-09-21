package org.paykesho.notificationservice.config;

import lombok.RequiredArgsConstructor;
import org.paykesho.notificationservice.security.RateLimitingFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
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
                        .antMatchers("/api/notifications/**", "/actuator/health").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("email")
                        .permitAll()
                )
                .logout(logout -> logout.permitAll())
                .rememberMe(remember -> remember
                        .key("${REMEMBER_ME_KEY:AbcDefgHijklmnOpqrs_1234567890}")
                        .tokenValiditySeconds(7 * 24 * 60 * 60)
                )
                .headers(headers -> headers
                        .frameOptions().sameOrigin()
                );
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/assets/**", "/images/**", "/webjars/**");
    }

}
