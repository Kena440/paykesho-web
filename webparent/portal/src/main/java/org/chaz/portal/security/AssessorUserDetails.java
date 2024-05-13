package org.chaz.portal.security;

import org.commons.entity.MemberAdmin;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class MemberAdminUserDetails implements UserDetails {
    private MemberAdmin memberAdmin;

    public MemberAdminUserDetails(MemberAdmin memberAdmin) {
        this.memberAdmin = memberAdmin;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    @Override
    public String getPassword() {
        return memberAdmin.getPassword();
    }

    @Override
    public String getUsername() {
        return memberAdmin.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return memberAdmin.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return memberAdmin.isEnabled();
    }

    public String getFullName() {
        return memberAdmin.getTitle() + " " + memberAdmin.getName();
    }

    public MemberAdmin getMemberAdmin() {
        return memberAdmin;
    }

}
