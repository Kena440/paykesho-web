package org.paykesho.clientportal.security;

import lombok.RequiredArgsConstructor;
import org.paykesho.clientportal.security.oauth.ClientOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
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

    private final ClientOAuth2UserService oAuth2UserService;
    // private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler; // Unused field
    // private final DatabaseLoginSuccessHandler databaseLoginSuccessHandler; // Unused field
    private final CustomLoginFailureHandler loginFailureHandler;

    private final RateLimitingFilter rateLimitingFilter;

    @Value("${paykesho.security.oauth2.enabled:false}")
    private boolean oauth2Enabled;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                .csrf(csrf -> csrf
                        .ignoringAntMatchers("/api/public/**") // Only public API endpoints
                )
                .authorizeRequests(authz -> authz
                        .antMatchers(HttpMethod.GET, "/clients/register", "/clients/verify",
                                "/business", "/how-it-works", "/faq", "/contact", "/reset-password", "/forgot-password/captcha").permitAll()
                        .antMatchers(HttpMethod.POST, "/clients/save-client", "/clients/rest/check-client", "/forgot-password", "/reset-password-action").permitAll()
                        .antMatchers("/actuator/health/**").permitAll()
                        .antMatchers("/clients/dashboard", "/vouchers/**", "/transactions/**").authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("email")
                        .successHandler(authenticationSuccessHandler())
                        .failureHandler(loginFailureHandler)
                        .permitAll());

        // Google/Facebook OAuth is intentionally optional for the initial deployment.
        // Enable it later with PAYKESHO_OAUTH2_ENABLED=true and the provider secrets.
        if (oauth2Enabled) {
            http.oauth2Login(oauth2 -> oauth2
                    .loginPage("/login")
                    .userInfoEndpoint(userInfo -> userInfo.userService(oAuth2UserService))
            );
        }

        http.logout(logout -> logout.permitAll())
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
        return new ClientUserDetailsService();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return new CustomAuthenticationSuccessHandler();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(authenticationProvider());
    }
}
