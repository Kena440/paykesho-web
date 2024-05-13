package org.membershipcommons.entity.members;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;
import org.membershipcommons.entity.IdBasedEntity;
import org.membershipcommons.enums.AuthenticationType;
import org.membershipcommons.enums.GenderType;

import javax.persistence.*;
import java.util.Date;
import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
@Entity
@Table(name = "member_admins")
public class MemberAdmin extends IdBasedEntity {
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

    public MemberAdmin() {
    }

    public MemberAdmin(Integer id) {
        this.id = id;
    }

    public MemberAdmin(String title, String name) {
        this.title = title;
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        MemberAdmin memberAdmin = (MemberAdmin) o;
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
