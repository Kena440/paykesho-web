package org.commons.entity.facilities;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.commons.entity.IdBasedEntity;
import org.commons.entity.State;

import javax.persistence.*;

@Data
@NoArgsConstructor
@Entity
@Table(name = "facilities")
public class Facility extends IdBasedEntity {

    @Column(name = "name", length = 245, nullable = false, unique = true)
    private String name;
    @ManyToOne
    @JoinColumn(name = "province_id")
    private State province;
    @Column(name = "district", length = 45, nullable = false)
    private String district;
    @ManyToOne
    @JoinColumn(name = "level_of_care_id")
    private LevelOfCare levelOfCare;
    @Column(length = 45)
    private String email;
    @ManyToOne
    @JoinColumn(name = "contact_person_id")
    private FacilityAdmin contactPerson;
    private boolean enabled;
    private boolean deleted;
    private float longitude;
    private float latitude;

    @Override
    public String toString() {
        return name + " ~ " + levelOfCare;
    }

}
