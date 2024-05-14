package org.chaz.portal.security;

import org.chaz.portal.assessors.repository.AssessorRepository;
import org.commons.entity.assessment.Assessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class AssessorUserDetailsService implements UserDetailsService {

    @Autowired
    private AssessorRepository assessorRepository;

    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        Assessor assessor = assessorRepository.findByEmail(s);

        if (assessor == null) throw new UsernameNotFoundException("No assessor found");

        return new AssessorUserDetails(assessor);

    }
}
