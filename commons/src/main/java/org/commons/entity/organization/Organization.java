package org.commons.entity.organization;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.commons.entity.IdBasedEntity;
import org.commons.entity.State;
import org.commons.enums.OrganizationType;

import javax.persistence.*;

@Data
@NoArgsConstructor
@Entity
@Table(name = "organizations")
public class Organization extends IdBasedEntity {

    @Column(name = "name", length = 245, nullable = false, unique = true)
    private String name;
    @ManyToOne
    @JoinColumn(name = "province_id")
    private State province;
    @Column(name = "district", length = 45, nullable = false)
    private String district;
    @Enumerated(EnumType.STRING)
    @Column(name = "organization_type", length = 50)
    private OrganizationType organizationType;
    @Column(length = 45)
    private String email;
    @ManyToOne
    @JoinColumn(name = "contact_person_id")
    private OrganizationAdmin contactPerson;
    private boolean enabled;
    private boolean deleted;
    private float longitude;
    private float latitude;

    @Override
    public String toString() {
        return name + " ~ " + organizationType;
    }

}
