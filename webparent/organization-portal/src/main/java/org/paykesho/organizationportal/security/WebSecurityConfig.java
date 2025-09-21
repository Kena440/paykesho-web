package org.paykesho.organizationportal.security;

import lombok.RequiredArgsConstructor;
import org.paykesho.organizationportal.security.oauth.OAuth2LoginSuccessHandler;
import org.paykesho.organizationportal.security.oauth.OrganizationAdminOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {


    private final OrganizationAdminOAuth2UserService oAuth2UserService;

    private final @Lazy OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    private final DatabaseLoginSuccessHandler databaseLoginSuccessHandler;

    private final CustomLoginFailureHandler loginFailureHandler;

    private final RateLimitingFilter rateLimitingFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                .csrf(csrf -> csrf
                        .ignoringAntMatchers("/api/dashboard/**", "/debug/**") // Only specific API endpoints
                )
                .authorizeRequests(authz -> authz
                        .antMatchers("/forgot-password", "/reset-password", "/reset-password-action").permitAll()
                        .antMatchers("/", "/account", "/assessors/update-account").authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("email")
                        .successHandler(databaseLoginSuccessHandler)
                        .failureHandler(loginFailureHandler)
                        .permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo.userService(oAuth2UserService))
                        .successHandler(oAuth2LoginSuccessHandler)
                )
                .logout(logout -> logout.permitAll())
                .rememberMe(remember -> remember
                        .key("${REMEMBER_ME_KEY:1234567890_aBcDeFgHiJkLmNoPqRsTuVwXyZ}")
                        .tokenValiditySeconds(14 * 24 * 60 * 60)
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .maximumSessions(3)
                        .maxSessionsPreventsLogin(false)
                        .and()
                        .sessionFixation().migrateSession()
                        .invalidSessionUrl("/login?expired")
                );
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/assets/**", "/images/**", "/webjars/**", "/js/**");
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new OrganizationAdminUserDetailsService();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }


}
