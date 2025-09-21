package org.paykesho.backoffice.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    private static final String ROLE_ADMIN = "Admin";
    private static final String ROLE_SUPERADMIN = "SuperAdmin";

    @Autowired
    @Lazy
    private CustomLoginFailureHandler loginFailureHandler;

    @Autowired
    @Lazy
    private CustomLoginSuccessHandler loginSuccessHandler;

    @Autowired
    private RateLimitingFilter rateLimitingFilter;

    @Bean
    public UserDetailsService userDetailsService() {
        return new MembershipMgtUserDetailsService();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(authenticationProvider());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeRequests(authz -> authz
                        .antMatchers("/events/registration/**", "/events/generateQRCode", "/assessments/registration/**",
                                "/assessments/generateQRCode", "/actuator/health", "/test/**", "/debug/**").permitAll()
                        .antMatchers("/actuator/**").hasAnyAuthority(ROLE_ADMIN, ROLE_SUPERADMIN)
                        .antMatchers("/facilities/save").hasAnyAuthority(ROLE_ADMIN, ROLE_SUPERADMIN)
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("email")
                        .successHandler(loginSuccessHandler)
                        .failureHandler(loginFailureHandler)
                        .permitAll()
                )
                .logout(logout -> logout.permitAll())
                .rememberMe(remember -> remember
                        .key("${REMEMBER_ME_KEY:AbcDefgHijklmnOpqrs_1234567890}")
                        .tokenValiditySeconds(7 * 24 * 60 * 60)
                )
                .headers(headers -> headers
                        .frameOptions().sameOrigin()
                        .contentTypeOptions().and()
                        .httpStrictTransportSecurity(hstsConfig -> hstsConfig.maxAgeInSeconds(31536000))
                )
                .sessionManagement(session -> session
                        .maximumSessions(5)
                        .maxSessionsPreventsLogin(false)
                );
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/assets/**", "/images/**", "/webjars/**");
    }

}
