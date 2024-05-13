package org.chaz.portal.security;

import org.chaz.portal.assessors.repository.AssessorRepository;
import org.commons.entity.facilities.MemberAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class MemberAdminUserDetailsService implements UserDetailsService {

    @Autowired
    private AssessorRepository assessorRepository;

    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        MemberAdmin memberAdmin = assessorRepository.findByEmail(s);

        if (memberAdmin == null) throw new UsernameNotFoundException("No user found");

        return new AssessorUserDetails(memberAdmin);

    }
}
