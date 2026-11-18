package org.commons.entity.kyc;

import lombok.Getter;
import lombok.Setter;
import org.commons.entity.client.Client;
import org.commons.enums.KycStatus;

import javax.persistence.*;
import java.time.Instant;

/**
 * A client's registration, held for back-office due diligence (KYC) before the
 * account is activated. Table: app.client_kyc_applications.
 */
@Getter
@Setter
@Entity
@Table(name = "client_kyc_applications")
public class ClientKycApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Set by the database (gen_random_uuid()); read-only here. */
    @Column(name = "uuid", insertable = false, updatable = false)
    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "first_name", length = 100, nullable = false)
    private String firstName;

    @Column(name = "middle_name", length = 100)
    private String middleName;

    @Column(name = "last_name", length = 100, nullable = false)
    private String lastName;

    @Column(name = "house_number", length = 50)
    private String houseNumber;

    @Column(name = "road", length = 150)
    private String road;

    @Column(name = "area", length = 150)
    private String area;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private KycStatus status = KycStatus.SUBMITTED;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    /** app.users.id of the back-office reviewer who decided. */
    @Column(name = "reviewed_by")
    private Integer reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "decision_reason")
    private String decisionReason;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (submittedAt == null) submittedAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    @Transient
    public String getFullName() {
        StringBuilder b = new StringBuilder(firstName == null ? "" : firstName);
        if (middleName != null && !middleName.isBlank()) b.append(' ').append(middleName);
        if (lastName != null) b.append(' ').append(lastName);
        return b.toString().trim();
    }

    @Transient
    public String getFullAddress() {
        StringBuilder b = new StringBuilder();
        for (String part : new String[]{houseNumber, road, area}) {
            if (part != null && !part.isBlank()) {
                if (b.length() > 0) b.append(", ");
                b.append(part.trim());
            }
        }
        return b.toString();
    }
}
