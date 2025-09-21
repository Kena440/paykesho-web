package org.paykesho.organizationportal.security;

import lombok.extern.slf4j.Slf4j;
import org.commons.entity.organization.OrganizationAdmin;
import org.paykesho.organizationportal.organizationadmins.repository.OrganizationAdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Slf4j
public class OrganizationAdminUserDetailsService implements UserDetailsService {

    @Autowired
    private OrganizationAdminRepository organizationAdminRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Loading user details for email: {}", email);

        OrganizationAdmin admin = organizationAdminRepository.findByEmail(email);

        if (admin == null) {
            log.warn("No organization admin found for email: {}", email);
            throw new UsernameNotFoundException("No organization admin found for email: " + email);
        }

        log.debug("Found organization admin: {} - Enabled: {}, AccountNonLocked: {}, FailedAttempts: {}",
                admin.getEmail(), admin.isEnabled(), admin.isAccountNonLocked(), admin.getFailedAttempt());

        return new OrganizationAdminUserDetails(admin);
    }
}
