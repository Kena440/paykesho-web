package org.commons.entity.cso;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.commons.entity.IdBasedEntity;
import org.commons.enums.AuthenticationType;
import org.commons.enums.GenderType;
import org.hibernate.Hibernate;

import javax.persistence.*;
import java.util.Date;
import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
@Entity
@Table(name = "cso_admins")
public class CSOAdmin extends IdBasedEntity {
    @Column(length = 15, nullable = false, name = "phone_number")
    protected String phoneNumber;
    @Column(length = 64, nullable = false)
    private String title;
    @Column(name = "name", length = 245, nullable = false)
    private String name;
    @Column(length = 45, nullable = false, unique = true)
    private String email;
    @Column(length = 64, nullable = false)
    private String password;
    @Column(name = "verification_code", length = 64)
    private String verificationCode;
    @Column(name = "reset_password_token", length = 30)
    private String resetPasswordToken;
    private boolean enabled;
    @Column(name = "created_time")
    private Date createdTime;
    @Enumerated(EnumType.STRING)
    @Column(name = "authentication_type", length = 10)
    private AuthenticationType authenticationType;
    @Enumerated(EnumType.STRING)
    @Column(name = "gender_type", length = 10)
    private GenderType genderType;
    @Column(name = "account_non_locked")
    private boolean accountNonLocked;

    @Column(name = "failed_attempt")
    private int failedAttempt;

    @Column(name = "lock_time")
    private Date lockTime;

    public CSOAdmin() {
    }

    public CSOAdmin(Integer id) {
        this.id = id;
    }

    public CSOAdmin(String title, String name) {
        this.title = title;
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        CSOAdmin memberAdmin = (CSOAdmin) o;
        return id != null && Objects.equals(id, memberAdmin.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Transient
    public String getFullName() {
        return title + ". " + name;
    }


}
