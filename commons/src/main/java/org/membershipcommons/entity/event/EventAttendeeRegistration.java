package org.membershipcommons.entity.event;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.membershipcommons.entity.IdBasedEntity;
import org.membershipcommons.entity.State;
import org.membershipcommons.entity.members.MemberInstitution;
import org.membershipcommons.enums.AttendeeType;
import org.membershipcommons.enums.InvoiceType;

import javax.persistence.*;
import java.util.Date;

@Data
@NoArgsConstructor
@Entity
@Table(name = "event_attendee_registration")
public class EventAttendeeRegistration extends IdBasedEntity {
    @ManyToOne
    @JoinColumn(name = "member_institution_id")
    private MemberInstitution memberInstitution;
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
    @Column(name = "title_position", length = 150, nullable = false)
    private String titlePosition;
    @Column(name = "nrc", length = 150, nullable = false)
    private String nrc;
    @ManyToOne
    @JoinColumn(name = "province_id")
    private State province;
    @Column(name = "district", length = 45, nullable = false)
    private String district;
    @Column(name = "registration_date")
    private Date registrationDate;
    @Column(name = "event_reg_id", length = 245)
    private String eventRegId;
    private boolean requiresAccomodation;
    private boolean approved;
    @Transient
    private String qrCode;
    @ManyToOne
    @JoinColumn(name = "event_accomodation_option_id")
    private EventAccomodationOption eventAccomodationOption;
    private boolean accomodationSharing;
    @Column(name = "pack_number")
    private String packNumber;
    @Enumerated(EnumType.STRING)
    private AttendeeType attendeeType;
}
