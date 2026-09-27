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
    private final AdminUserDetailsService userDetailsService;
    public AdminAuthSecurityConfig(AdminUserDetailsService userDetailsService){this.userDetailsService=userDetailsService;}
    @Bean public PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
    @Override protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
            .antMatchers("/login","/assets/**","/images/**","/webjars/**","/actuator/health").permitAll()
            .anyRequest().authenticated()
            .and().userDetailsService(userDetailsService)
            .formLogin().loginPage("/login").loginProcessingUrl("/login")
            .usernameParameter("email").defaultSuccessUrl("/",true)
            .failureUrl("/login?error=true").permitAll()
            .and().logout().logoutSuccessUrl("/login?logout=true").permitAll()
            .and().rememberMe().key("paykesho-admin-remember-2026").tokenValiditySeconds(604800);
    }
}
