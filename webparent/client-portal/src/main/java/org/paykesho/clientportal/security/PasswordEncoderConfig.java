package org.paykesho.clientportal.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Kept out of WebSecurityConfig: ClientService needs the encoder and the
 * login failure handler (a WebSecurityConfig dependency) needs ClientService,
 * so defining it there formed a cycle that broke the first request.
 */
@Configuration
public class PasswordEncoderConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
