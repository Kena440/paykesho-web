package org.paykesho.backoffice.adminauth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
public class AdminAuthSecurityConfig extends WebSecurityConfigurerAdapter {

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
    private final AdminUserDetailsService userDetailsService;
    public AdminAuthSecurityConfig(AdminUserDetailsService userDetailsService){this.userDetailsService=userDetailsService;}
    @Bean public PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
    @Override protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
            .antMatchers("/login","/admin/login","/ping","/assets/**","/images/**","/webjars/**","/actuator/health").permitAll()
            .anyRequest().authenticated()
            .and().userDetailsService(userDetailsService)
            .formLogin().loginPage("/admin/login").loginProcessingUrl("/login")
            .usernameParameter("email").defaultSuccessUrl("/",true)
            .failureUrl("/admin/login?error=true").permitAll()
            .and().logout().logoutSuccessUrl("/admin/login?logout=true").permitAll()
            .and().rememberMe().key(rememberMeKey()).tokenValiditySeconds(604800);
    }
}
