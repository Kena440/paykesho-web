package org.paykesho.backoffice.adminauth;

import org.paykesho.backoffice.adminauth.entity.AdminRole;
import org.paykesho.backoffice.adminauth.entity.AdminUser;
import org.paykesho.backoffice.adminauth.repository.AdminUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdminUserDetailsService implements UserDetailsService {
    private final AdminUserRepository repository;
    private final BCryptPasswordEncoder bootstrapPasswordEncoder = new BCryptPasswordEncoder();

    @Value("${PAYKESHO_ADMIN_EMAIL:}")
    private String configuredAdminEmail;

    @Value("${PAYKESHO_ADMIN_PASSWORD:}")
    private String configuredAdminPassword;

    public AdminUserDetailsService(AdminUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Cloudflare supplies the bootstrap administrator credentials as secrets.
        // Prefer this credential pair when configured so the first administrator
        // can sign in without requiring a pre-seeded users row in the database.
        if (isBootstrapAdminConfigured() && configuredAdminEmail.equalsIgnoreCase(email)) {
            return new org.springframework.security.core.userdetails.User(
                configuredAdminEmail,
                bootstrapPasswordEncoder.encode(configuredAdminPassword),
                true,
                true,
                true,
                true,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
            );
        }

        AdminUser user = repository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("Invalid administrator credentials"));

        if (user.isDeleted() || !user.isEnabled()) {
            throw new DisabledException("Administrator account is disabled");
        }

        if (!user.isAccountNonLocked()) {
            throw new LockedException("Administrator account is locked");
        }

        List<GrantedAuthority> authorities = new ArrayList<>();
        for (AdminRole role : user.getRoles()) {
            if (!role.isDeleted() && role.getName() != null) {
                authorities.add(new SimpleGrantedAuthority(role.getName()));
            }
        }

        return new org.springframework.security.core.userdetails.User(
            user.getEmail(),
            user.getPassword(),
            user.isEnabled(),
            true,
            true,
            user.isAccountNonLocked(),
            authorities
        );
    }

    private boolean isBootstrapAdminConfigured() {
        return configuredAdminEmail != null
            && !configuredAdminEmail.isBlank()
            && configuredAdminPassword != null
            && !configuredAdminPassword.isBlank();
    }
}
