package org.commons.entity.kyc;

import lombok.Getter;
import lombok.Setter;
import org.commons.enums.KycDocumentType;

import javax.persistence.*;
import java.time.Instant;

/**
 * A file (NRC, payslip, selfie) attached to a KYC application. The file itself
 * is in app.file_uploads; this row links it to the client and application.
 * Table: app.kyc_documents.
 */
@Getter
@Setter
@Entity
@Table(name = "kyc_documents")
public class KycDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", insertable = false, updatable = false)
    private String uuid;

    @Column(name = "client_id", nullable = false)
    private Integer clientId;

    @Column(name = "kyc_application_id")
    private Long kycApplicationId;

    @Column(name = "voucher_id")
    private Integer voucherId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", length = 20, nullable = false)
    private KycDocumentType documentType;

    @Column(name = "file_upload_id", nullable = false)
    private Long fileUploadId;

    /** PENDING, ACCEPTED or REJECTED. */
    @Column(name = "status", length = 20, nullable = false)
    private String status = "PENDING";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
