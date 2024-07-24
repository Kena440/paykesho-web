package org.chaz.csoportal.security;

import org.chaz.csoportal.csoadmins.repository.CSOAdminRepository;
import org.commons.entity.cso.CSOAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class CSOAdminUserDetailsService implements UserDetailsService {

    @Autowired
    private CSOAdminRepository assessorRepository;

    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        CSOAdmin assessor = assessorRepository.findByEmail(s);

        if (assessor == null) throw new UsernameNotFoundException("No assessor found");

        return new CSOAdminUserDetails(assessor);

    }
}
