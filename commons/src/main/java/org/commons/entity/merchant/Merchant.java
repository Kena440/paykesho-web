package org.commons.entity.merchant;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.commons.constants.Constants;
import org.commons.entity.IdBasedEntity;
import org.commons.entity.State;
import org.commons.enums.MerchantType;

import javax.persistence.*;
import java.util.Date;

@Data
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
    @ManyToOne
    @JoinColumn(name = "province_id")
    private State province;
    @Column(name = "district")
    private String district;
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
    private float longitude;
    private float latitude;
    @Column(length = 64)
    private String photos;
    @Column(name = "remote_file_id")
    private Integer remoteFileId;
    private String photoPath;

    @Override
    public String toString() {
        return name + " ~ " + merchantType;
    }

    @Transient
    public String getPhotosImagePath() {
        if (id == null || photos == null) {
            return "/images/default_image.png";
        }
        return Constants.S3_BASE_URI + "/merchant_logos/" + this.id + "/" + this.photos;
    }

}
