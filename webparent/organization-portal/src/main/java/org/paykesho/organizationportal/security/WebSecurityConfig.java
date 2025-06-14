package org.paykesho.organizationportal.security;

import lombok.RequiredArgsConstructor;
import org.paykesho.organizationportal.security.oauth.OAuth2LoginSuccessHandler;
import org.paykesho.organizationportal.security.oauth.OrganizationAdminOAuth2UserService;
import org.springframework.beans.factory.annotation.Autowired;
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

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {


    private final OrganizationAdminOAuth2UserService oAuth2UserService;

    private final @Lazy OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    private final DatabaseLoginSuccessHandler databaseLoginSuccessHandler;

    private final CustomLoginFailureHandler loginFailureHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf()
                .ignoringAntMatchers("/clients/rest/**")
                .and().authorizeRequests().antMatchers("/forgot-password", "/reset-password", "/reset-password-action").permitAll().antMatchers("/", "/account", "/assessors/update-account").authenticated().anyRequest().authenticated().and().formLogin().loginPage("/login").usernameParameter("email").successHandler(databaseLoginSuccessHandler).failureHandler(loginFailureHandler).permitAll().and().oauth2Login().loginPage("/login").userInfoEndpoint().userService(oAuth2UserService).and().successHandler(oAuth2LoginSuccessHandler).and().logout().permitAll().and().rememberMe().key("1234567890_aBcDeFgHiJkLmNoPqRsTuVwXyZ").tokenValiditySeconds(14 * 24 * 60 * 60);
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.ALWAYS);
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/assets/**", "/images/**", "/webjars/**", "/js/**");
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new OrganizationAdminUserDetailsService();
    }

    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }


}
