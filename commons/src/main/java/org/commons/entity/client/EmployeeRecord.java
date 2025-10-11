package org.commons.entity.client;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.commons.entity.IdBasedEntity;
import org.commons.entity.organization.Organization;

import javax.persistence.*;
import java.util.Date;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "employee_records")
public class EmployeeRecord extends IdBasedEntity {
    @Column(name = "name", length = 128, nullable = false)
    private String name;
    @Column(name = "nrc", length = 128, nullable = false, unique = true)
    private String nrc;
    @Column(name = "tpin", length = 128, nullable = false, unique = true)
    private String tpin;
    @Column(name = "man_number", length = 128, nullable = false, unique = true)
    private String manNumber;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "organization_id")
    private Organization organization;
    private Date registrationDate;
}
