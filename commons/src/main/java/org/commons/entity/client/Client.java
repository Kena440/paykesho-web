package org.commons.entity.client;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.commons.entity.IdBasedEntity;
import org.commons.entity.organization.Organization;
import org.commons.enums.AuthenticationType;
import org.commons.enums.ClientType;
import org.commons.enums.GenderType;
import org.commons.enums.SalutationType;
import org.hibernate.Hibernate;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
@Entity
@Table(name = "clients")
public class Client extends IdBasedEntity {
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9+\\-\\s()]{7,15}$", message = "Invalid phone number format")
    @Column(length = 15, nullable = false, name = "phone_number")
    protected String phoneNumber;
    
    @NotBlank(message = "Address is required")
    @Size(min = 5, max = 150, message = "Address must be between 5 and 150 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\s,.-]+$", message = "Address contains invalid characters")
    @Column(length = 150, nullable = false, name = "address")
    protected String address;
    
    @Column(name = "code", length = 128, unique = true)
    private String code;
    
    @NotBlank(message = "TPIN is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "TPIN must be exactly 10 digits")
    @Column(name = "tpin", length = 128, nullable = false, unique = true)
    private String tpin;
    
    @NotBlank(message = "MAN number is required")
    @Pattern(regexp = "^[0-9]{1,10}$", message = "MAN number must be numeric and up to 10 digits")
    @Column(name = "man_number", length = 128, nullable = false, unique = true)
    private String manNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "salutation", length = 10)
    private SalutationType salutation;
    
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 245, message = "Name must be between 2 and 245 characters")
    @Pattern(regexp = "^[a-zA-Z\\s'-]+$", message = "Name can only contain letters, spaces, hyphens and apostrophes")
    @Column(name = "name", length = 245, nullable = false)
    private String name;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 45, message = "Email must not exceed 45 characters")
    @Column(length = 45, nullable = false, unique = true)
    private String email;
    
    @NotBlank(message = "NRC is required")
    @Pattern(regexp = "^[0-9]{6}/[0-9]{2}/[0-9]{1}$", message = "NRC must be in format XXXXXX/XX/X")
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
    @Column(name = "client_income", length = 128, nullable = false)
    private BigDecimal clientIncome;
    @Column(name = "failed_attempt")
    private int failedAttempt;

    @Column(name = "lock_time")
    private Date lockTime;
    @ManyToOne
    @JoinColumn(name = "organization_id")
    private Organization organization;
    @Column(name = "remote_file_id")
    private Integer remoteFileId;
    @Column(length = 512)
    private String photoPath;
    @Column(length = 512)
    private String photos;
    @Transient
    private ClientBalance clientBalance;
    @Transient
    private Integer creditScore;

    public Client() {
    }

    public Client(Integer id) {
        this.id = id;
    }

    public Client(SalutationType salutation, String name) {
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

    @Transient
    public String getPhotoPath() {
        if (id == null || photos == null) {
            return "/images/default_user_image.jpg";
        }
        return this.photoPath;
    }

    @Override
    public String toString() {
        return salutation + ". " + name;
    }
}
