package org.paykesho.merchantportal.security;

import org.commons.entity.merchant.MerchantAdmin;
import org.paykesho.merchantportal.merchantadmins.repository.MerchantAdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class MerchantAdminUserDetailsService implements UserDetailsService {

    @Autowired
    private MerchantAdminRepository assessorRepository;

    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        MerchantAdmin assessor = assessorRepository.findByEmail(s);

        if (assessor == null) throw new UsernameNotFoundException("No assessor found");

        return new MerchantAdminUserDetails(assessor);

    }
}
