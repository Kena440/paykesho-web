package org.commons.entity.settings;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.commons.entity.IdBasedEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "departments")
public class Department extends IdBasedEntity {
    @Column(name = "department_name", length = 45, nullable = false, unique = true)
    private String departmentName;

    @Column(name = "department_code", length = 145, nullable = false)
    private String departmentCode;

    @Column(name = "hod_email", length = 145, nullable = false, unique = true)
    private String hodEmail;

    private boolean enabled;
    private boolean deleted;
}
