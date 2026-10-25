package org.paykesho.backoffice.security;

import org.commons.entity.Role;
import org.commons.entity.User;
import org.paykesho.backoffice.user.repository.RoleRepository;
import org.paykesho.backoffice.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.StreamSupport;

/**
 * Creates the first back-office administrator from the Cloudflare secrets
 * PAYKESHO_ADMIN_EMAIL / PAYKESHO_ADMIN_PASSWORD when no user with that email
 * exists yet. An existing user is never modified, and sign-in always uses the
 * password hash stored in the database.
 */
@Component
public class AdminBootstrap {

    static final String ADMIN_ROLE = "Admin";
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final String email;
    private final String password;

    public AdminBootstrap(UserRepository userRepository,
                          RoleRepository roleRepository,
                          @Value("${paykesho.admin.bootstrap-email:}") String email,
                          @Value("${paykesho.admin.bootstrap-password:}") String password) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.email = email == null ? "" : email.trim();
        this.password = password == null ? "" : password;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void ensureAdministratorExists() {
        if (email.isEmpty() || password.isEmpty()) {
            return;
        }
        try {
            if (userRepository.getUserByEmail(email) != null) {
                return;
            }
            Role adminRole = StreamSupport.stream(roleRepository.findAll().spliterator(), false)
                .filter(r -> ADMIN_ROLE.equalsIgnoreCase(r.getName()))
                .findFirst()
                .orElseGet(() -> roleRepository.save(new Role(ADMIN_ROLE, "PayKesho back-office administrator")));

            User admin = new User(email, new BCryptPasswordEncoder().encode(password), "PayKesho", "Administrator");
            admin.setEnabled(true);
            admin.setAccountNonLocked(true);
            admin.addRole(adminRole);
            userRepository.save(admin);
            LOGGER.info("Created bootstrap back-office administrator account");
        } catch (RuntimeException e) {
            // Never stop the back office from starting because of the bootstrap.
            LOGGER.error("Could not create bootstrap administrator account", e);
        }
    }
}
