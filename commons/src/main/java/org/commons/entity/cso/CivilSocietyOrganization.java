package org.commons.entity.cso;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.commons.entity.IdBasedEntity;
import org.commons.entity.State;
import org.commons.entity.facilities.LevelOfCare;

import javax.persistence.*;
import java.util.Date;

@Data
@NoArgsConstructor
@Entity
@Table(name = "civil_society_organizations")
public class CivilSocietyOrganization extends IdBasedEntity {
    @Column(name = "name", length = 245, nullable = false, unique = true)
    private String name;
    @Column(length = 15, name = "phone_number")
    protected String phoneNumber;
    @Column(length = 10, name = "postal_code")
    protected String postalCode;
    @Column(length = 150, name = "postal_address")
    protected String postalAddress;
    @ManyToOne
    @JoinColumn(name = "province_id")
    private State province;
    @Column(name = "district")
    private String district;
    @Column(name = "services_offered")
    protected String servicesOffered;
    @ManyToOne
    @JoinColumn(name = "contact_person_id")
    private CSOAdmin contactPerson;
    private Date registrationDate;
    @Column(name = "funders")
    private String funders;
    @Column(name = "partners")
    private String partners;
    private Date membershipZicsip;
    @Column(name = "objectives")
    private String objectives;
    @Column(length = 45)
    private String email;
    private boolean enabled;
    private boolean deleted;
    private float longitude;
    private float latitude;

    @Override
    public String toString() {
        return name + " ~ " + email;
    }

}
