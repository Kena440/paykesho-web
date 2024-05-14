package org.chaz.backoffice.security;


import org.chaz.backoffice.user.repository.UserRepository;
import org.commons.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class MembershipMgtUserDetailsService implements UserDetailsService {
    @Autowired
    UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        User user = userRepository.getUserByEmail(s);

        if (user != null) {
            return new MembershipMgtUserDetails(user);
        }
        throw new UsernameNotFoundException("No user with email" + s);
    }
}
