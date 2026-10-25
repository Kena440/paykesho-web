package org.commons.entity.merchant;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.commons.entity.IdBasedEntity;
import org.commons.enums.MerchantType;

import javax.persistence.*;
import java.util.Date;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@Entity
@Table(name = "merchants")
public class Merchant extends IdBasedEntity {
    @Column(length = 15, name = "phone_number")
    protected String phoneNumber;
    @Column(length = 10, name = "postal_code")
    protected String postalCode;
    @Column(length = 150, name = "physical_address")
    protected String physicalAddress;
    @Column(name = "services_offered")
    protected String servicesOffered;
    @Column(name = "code", length = 128, nullable = false, unique = true)
    private String code;
    @Column(name = "name", length = 50, nullable = false, unique = true)
    private String name;
    @Column(name = "details", length = 128, nullable = false)
    private String details;
    @Enumerated(EnumType.STRING)
    @Column(name = "merchant_type", length = 50)
    private MerchantType merchantType;
    @ManyToOne
    @JoinColumn(name = "contact_person_id")
    private MerchantAdmin contactPerson;
    private Date registrationDate;
    @Column(length = 45)
    private String email;
    private boolean enabled;
    private boolean deleted;
    // Wrapper types: these columns are nullable, and a primitive float made every
    // query that loaded a record without coordinates fail. Accessors keep the
    // original float signatures (0 when unset) for existing callers.
    private Float longitude;
    private Float latitude;

    @Column(length = 512)
    private String photos;
    @Column(name = "remote_file_id")
    private Integer remoteFileId;
    @Column(name = "photo_path", length = 512)
    private String photoPath;

    // Lenco payout routing. These fields are intentionally optional so existing
    // merchants remain valid while their payout profile is completed.
    @Column(name = "lenco_transfer_recipient_id", length = 64)
    private String lencoTransferRecipientId;
    @Column(name = "payout_method", length = 32)
    private String payoutMethod; // mobile-money or bank-account
    @Column(name = "payout_operator", length = 16)
    private String payoutOperator; // airtel, mtn, zamtel
    @Column(name = "payout_phone", length = 20)
    private String payoutPhone;
    @Column(name = "payout_bank_id", length = 64)
    private String payoutBankId;
    @Column(name = "payout_account_number", length = 64)
    private String payoutAccountNumber;

    @Override
    public String toString() {
        return name + " ~ " + merchantType;
    }

    @Transient
    public String getPhotoPath() {
        if (this.photoPath != null && !this.photoPath.trim().isEmpty()) {
            return this.photoPath;
        }
        return "/images/default_image.png";
    }

    public float getLongitude() {
        return longitude == null ? 0f : longitude;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public float getLatitude() {
        return latitude == null ? 0f : latitude;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }
}
