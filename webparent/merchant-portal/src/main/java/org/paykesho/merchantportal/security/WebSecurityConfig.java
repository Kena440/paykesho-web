package org.paykesho.merchantportal.security;

import lombok.RequiredArgsConstructor;
import org.paykesho.merchantportal.security.oauth.MerchantAdminOAuth2UserService;
import org.paykesho.merchantportal.security.oauth.OAuth2LoginSuccessHandler;
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

    // Remember-me signing key comes from the REMEMBER_ME_KEY secret. If it is not
    // set, a random per-start key is used so no hard-coded key can ever sign cookies.
    @org.springframework.beans.factory.annotation.Value("${REMEMBER_ME_KEY:}")
    private String configuredRememberMeKey;

    private String rememberMeKey() {
        if (configuredRememberMeKey != null && !configuredRememberMeKey.trim().isEmpty()) {
            return configuredRememberMeKey;
        }
        byte[] bytes = new byte[32];
        new java.security.SecureRandom().nextBytes(bytes);
        configuredRememberMeKey = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return configuredRememberMeKey;
    }


    private final MerchantAdminOAuth2UserService oAuth2UserService;
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
                .authorizeRequests(authz -> authz
                        .antMatchers("/forgot-password", "/forgot-password/captcha", "/reset-password", "/reset-password-action").permitAll()
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
                        .key(rememberMeKey())
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
        return new MerchantAdminUserDetailsService();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }


}
