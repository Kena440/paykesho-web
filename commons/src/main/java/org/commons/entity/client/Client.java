package org.commons.entity.client;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.commons.entity.IdBasedEntity;
import org.commons.entity.organization.Organization;
import org.commons.enums.AuthenticationType;
import org.commons.enums.ClientType;
import org.commons.enums.GenderType;
import org.hibernate.Hibernate;

import javax.persistence.*;
import java.util.Date;
import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
@Entity
@Table(name = "clients")
public class Client extends IdBasedEntity {
    @Column(length = 15, nullable = false, name = "phone_number")
    protected String phoneNumber;
    @Column(length = 150, nullable = false, name = "address")
    protected String address;
    @Column(name = "code", length = 128, nullable = false, unique = true)
    private String code;
    @Column(length = 64, nullable = false)
    private String salutation;
    @Column(name = "name", length = 245, nullable = false)
    private String name;
    @Column(length = 45, nullable = false, unique = true)
    private String email;
    @Column(length = 45, nullable = false, unique = true)
    private String nrc;
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
    @Enumerated(EnumType.STRING)
    @Column(name = "client_type", length = 10)
    private ClientType clientType;
    @Column(name = "account_non_locked")
    private boolean accountNonLocked;

    @Column(name = "failed_attempt")
    private int failedAttempt;

    @Column(name = "lock_time")
    private Date lockTime;
    @OneToOne
    @JoinColumn(name = "organization_id")
    private Organization organization;

    public Client() {
    }

    public Client(Integer id) {
        this.id = id;
    }

    public Client(String salutation, String name) {
        this.salutation = salutation;
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Client memberAdmin = (Client) o;
        return id != null && Objects.equals(id, memberAdmin.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Transient
    public String getFullName() {
        return salutation + ". " + name;
    }


}
