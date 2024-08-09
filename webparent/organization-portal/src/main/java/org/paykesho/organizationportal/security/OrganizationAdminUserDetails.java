package org.paykesho.organizationportal.security;

import org.commons.entity.organization.OrganizationAdmin;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class OrganizationAdminUserDetails implements UserDetails {
    private OrganizationAdmin organizationAdmin;

    public OrganizationAdminUserDetails(OrganizationAdmin organizationAdmin) {
        this.organizationAdmin = organizationAdmin;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    @Override
    public String getPassword() {
        return organizationAdmin.getPassword();
    }

    @Override
    public String getUsername() {
        return organizationAdmin.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return organizationAdmin.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return organizationAdmin.isEnabled();
    }

    public String getFullName() {
        return organizationAdmin.getTitle() + " " + organizationAdmin.getName();
    }

    public OrganizationAdmin getOrganizationAdmin() {
        return organizationAdmin;
    }

}
