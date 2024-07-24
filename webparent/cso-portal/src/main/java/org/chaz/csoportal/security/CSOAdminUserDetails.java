package org.chaz.csoportal.security;

import org.commons.entity.cso.CSOAdmin;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class CSOAdminUserDetails implements UserDetails {
    private CSOAdmin csoAdmin;

    public CSOAdminUserDetails(CSOAdmin csoAdmin) {
        this.csoAdmin = csoAdmin;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    @Override
    public String getPassword() {
        return csoAdmin.getPassword();
    }

    @Override
    public String getUsername() {
        return csoAdmin.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return csoAdmin.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return csoAdmin.isEnabled();
    }

    public String getFullName() {
        return csoAdmin.getTitle() + " " + csoAdmin.getName();
    }

    public CSOAdmin getCSOAdmin() {
        return csoAdmin;
    }

}
