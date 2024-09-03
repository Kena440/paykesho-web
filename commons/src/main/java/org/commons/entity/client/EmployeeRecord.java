package org.commons.entity.client;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.commons.entity.organization.Organization;

import javax.persistence.*;
import java.util.Date;

@Data
@NoArgsConstructor
@Entity
@Table(name = "employee_records")
public class EmployeeRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "name", length = 128, nullable = false)
    private String name;
    @Column(name = "nrc", length = 128, nullable = false, unique = true)
    private String nrc;
    @Column(name = "tpin", length = 128, nullable = false, unique = true)
    private String tpin;
    @Column(name = "man_number", length = 128, nullable = false, unique = true)
    private String manNumber;
    @OneToOne
    @JoinColumn(name = "organization_id")
    private Organization organization;
    private Date registrationDate;
}
