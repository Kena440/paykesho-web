package org.paykesho.backoffice.adminauth;

import org.paykesho.backoffice.adminauth.entity.AdminRole;
import org.paykesho.backoffice.adminauth.entity.AdminUser;
import org.paykesho.backoffice.adminauth.repository.AdminUserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminUserDetailsService implements UserDetailsService {
    private final AdminUserRepository repository;
    public AdminUserDetailsService(AdminUserRepository repository){this.repository=repository;}
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AdminUser user=repository.findByEmail(email)
            .orElseThrow(()->new UsernameNotFoundException("Invalid administrator credentials"));
        if(user.isDeleted() || !user.isEnabled()) throw new DisabledException("Administrator account is disabled");
        if(!user.isAccountNonLocked()) throw new LockedException("Administrator account is locked");
        List<GrantedAuthority> authorities=new ArrayList<>();
        for(AdminRole role:user.getRoles()){
            if(!role.isDeleted() && role.getName()!=null) authorities.add(new SimpleGrantedAuthority(role.getName()));
        }
        return new org.springframework.security.core.userdetails.User(
            user.getEmail(),user.getPassword(),user.isEnabled(),true,true,user.isAccountNonLocked(),authorities);
    }
}
