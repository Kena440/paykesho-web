package org.paykesho.clientportal.admin;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
@Order(1)
public class AdminLoginSecurityConfig extends WebSecurityConfigurerAdapter {

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
    @Value("${PAYKESHO_ADMIN_EMAIL:}") private String adminEmail;
    @Value("${PAYKESHO_ADMIN_PASSWORD:}") private String adminPassword;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private UserDetailsService adminUserDetailsService() {
        if (adminEmail == null || adminEmail.trim().isEmpty() || adminPassword == null || adminPassword.isEmpty()) {
            throw new IllegalStateException("PAYKESHO_ADMIN_EMAIL and PAYKESHO_ADMIN_PASSWORD must be configured");
        }
        return new InMemoryUserDetailsManager(
            User.withUsername(adminEmail.trim())
                .password(passwordEncoder.encode(adminPassword))
                .roles("ADMIN")
                .build()
        );
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.antMatcher("/admin/**")
            .authorizeRequests()
                .antMatchers("/admin/login", "/admin/assets/**", "/admin/images/**").permitAll()
                .anyRequest().hasRole("ADMIN")
                .and()
            .userDetailsService(adminUserDetailsService())
            .formLogin()
                .loginPage("/admin/login")
                .loginProcessingUrl("/admin/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .defaultSuccessUrl("/admin", true)
                .failureUrl("/admin/login?error=true")
                .permitAll()
                .and()
            .logout()
                .logoutUrl("/admin/logout")
                .logoutSuccessUrl("/admin/login?logout=true")
                .permitAll()
                .and()
            .rememberMe()
                .key(rememberMeKey())
                .tokenValiditySeconds(604800);
    }
}
