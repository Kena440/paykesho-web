package org.membershipcommons.entity.event;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.membershipcommons.entity.IdBasedEntity;
import org.membershipcommons.enums.PaymentMethod;

import javax.persistence.*;
import java.util.Date;

@Data
@NoArgsConstructor
@Entity
@Table(name = "event_vendor_registration")
public class EventVendorRegistration extends IdBasedEntity {
    @ManyToOne
    @JoinColumn(name = "event_vendor_package_id")
    private EventVendorPackage eventVendorPackage;
    @ManyToOne
    @JoinColumn(name = "event_id")
    private Event event;
    @Column(length = 128, nullable = false)
    private String name;
    @Column(name = "phone_number", length = 28, nullable = false)
    private String phoneNumber;
    @Column(name = "email", length = 100, nullable = false)
    private String email;
    @Column(name = "organization", length = 150, nullable = false)
    private String organization;
    @Column(name = "registration_date")
    private Date registrationDate;
    @Column(name = "event_reg_id", length = 245)
    private String eventRegId;
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;
    private boolean approved;
    @Transient
    private String qrCode;

}
