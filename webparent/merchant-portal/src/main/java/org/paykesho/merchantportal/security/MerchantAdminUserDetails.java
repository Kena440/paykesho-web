package org.paykesho.merchantportal.security;

import org.commons.entity.merchant.MerchantAdmin;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class MerchantAdminUserDetails implements UserDetails {
    private MerchantAdmin merchantAdmin;

    public MerchantAdminUserDetails(MerchantAdmin merchantAdmin) {
        this.merchantAdmin = merchantAdmin;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    @Override
    public String getPassword() {
        return merchantAdmin.getPassword();
    }

    @Override
    public String getUsername() {
        return merchantAdmin.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return merchantAdmin.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return merchantAdmin.isEnabled();
    }

    public String getFullName() {
        return merchantAdmin.getTitle() + " " + merchantAdmin.getName();
    }

    public MerchantAdmin getMerchantAdmin() {
        return merchantAdmin;
    }

}
