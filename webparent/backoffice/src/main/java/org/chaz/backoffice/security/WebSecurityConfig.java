package org.chaz.backoffice.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    private static final String ROLE_ADMIN = "Admin";
    private static final String ROLE_SUPERADMIN = "SuperAdmin";
    private static final String ROLE_MEMBERSHIP = "Membership";
    private static final String ROLE_MICROFIN = "MicroFin";
    private static final String ROLE_FINANCE = "Finance";
    private static final String ROLE_ME = "MonEvaluation";
    private static final String ROLE_GENERAL = "General";

//    @Autowired
//    private CustomLoginFailureHandler loginFailureHandler;

    @Bean
    public UserDetailsService userDetailsService() {
        return new MembershipMgtUserDetailsService();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

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
        http.authorizeRequests().antMatchers("/events/registration/**", "/events/generateQRCode", "/general-invoices/view/**").permitAll().antMatchers("/member-institutions/save").hasAnyAuthority(ROLE_ADMIN, ROLE_MEMBERSHIP, ROLE_SUPERADMIN).antMatchers("/iga/save-repayment-record", "/iga/save").hasAnyAuthority(ROLE_ADMIN, ROLE_MEMBERSHIP, ROLE_MICROFIN, ROLE_SUPERADMIN).anyRequest().authenticated().and().formLogin().loginPage("/login").usernameParameter("email")
//                .failureHandler(loginFailureHandler)
                .permitAll().and().logout().permitAll().and().rememberMe().key("AbcDefgHijklmnOpqrs_1234567890").tokenValiditySeconds(7 * 24 * 60 * 60);
        http.headers().frameOptions().sameOrigin();

    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/assets/**", "/images/**", "/webjars/**");
    }

}
