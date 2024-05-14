package org.chaz.portal.security;

import org.commons.entity.Assessor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class AssessorUserDetails implements UserDetails {
    private Assessor assessor;

    public AssessorUserDetails(Assessor assessor) {
        this.assessor = assessor;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    @Override
    public String getPassword() {
        return assessor.getPassword();
    }

    @Override
    public String getUsername() {
        return assessor.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return assessor.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return assessor.isEnabled();
    }

    public String getFullName() {
        return assessor.getTitle() + " " + assessor.getName();
    }

    public Assessor getAssessor() {
        return assessor;
    }

}
