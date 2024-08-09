package org.paykesho.organizationportal.security;

import org.commons.entity.organization.OrganizationAdmin;
import org.paykesho.organizationportal.organizationadmins.repository.OrganizationAdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class OrganizationAdminUserDetailsService implements UserDetailsService {

    @Autowired
    private OrganizationAdminRepository organizationAdminRepository;

    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        OrganizationAdmin assessor = organizationAdminRepository.findByEmail(s);

        if (assessor == null) throw new UsernameNotFoundException("No assessor found");

        return new OrganizationAdminUserDetails(assessor);

    }
}
