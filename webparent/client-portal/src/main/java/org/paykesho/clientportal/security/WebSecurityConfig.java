package org.paykesho.clientportal.security;

import lombok.RequiredArgsConstructor;
import org.paykesho.clientportal.security.oauth.ClientOAuth2UserService;
import org.paykesho.clientportal.security.oauth.OAuth2LoginSuccessHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    private final ClientOAuth2UserService oAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final DatabaseLoginSuccessHandler databaseLoginSuccessHandler;
    private final CustomLoginFailureHandler loginFailureHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable().authorizeRequests().antMatchers(HttpMethod.GET, "/api/**", "/clients/register", "/clients/save-client", "/clients/verify", "/business", "/how-it-works", "/faq", "/contact").permitAll();
        http.authorizeRequests().antMatchers("/clients/dashboard", "/vouchers/**", "/transactions/**").authenticated().and().formLogin().loginPage("/login").usernameParameter("email").successHandler(authenticationSuccessHandler()).failureHandler(loginFailureHandler).permitAll().and().oauth2Login().loginPage("/login").userInfoEndpoint().userService(oAuth2UserService).and().and().logout().permitAll().and().rememberMe().key("1234567890_aBcDeFgHiJkLmNoPqRsTuVwXyZ").tokenValiditySeconds(14 * 24 * 60 * 60);

        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.ALWAYS);
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/assets/**", "/images/**", "/webjars/**", "/js/**");
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new ClientUserDetailsService();
    }

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
